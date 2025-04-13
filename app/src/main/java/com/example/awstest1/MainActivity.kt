package com.example.awstest1

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.iot.AWSIotClient
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.security.KeyStore
import java.util.UUID


class MainActivity : Activity() {
    private var listView: ListView? = null
    private var xbeeNodesList: MutableList<String> = mutableListOf()

    var txtMessage: String? = null
    var tvStatus1: TextView? = null
    var tvStatus: TextView? = null
    var btnPublish: Button? = null
    var builder: AlertDialog.Builder? = null
    var mIotAndroidClient: AWSIotClient? = null
    var mqttManager: AWSIotMqttManager? = null
    var clientId: String? = null
    var keystorePath: String? = null
    var keystoreName: String? = null
    var keystorePassword: String? = null
    var clientKeyStore: KeyStore? = null
    var certificateId: String? = null
    var credentialsProvider: CognitoCachingCredentialsProvider? = null
    var progressBar: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.theme)
        }
        progressBar = findViewById(R.id.progressBar3)
        progressBar?.visibility = View.GONE
        listView = findViewById(R.id.listView)
        txtMessage = "{'command':'Scan'}"
        btnPublish = findViewById<View>(R.id.btnPublish) as Button
        btnPublish!!.setOnClickListener{
            progressBar?.visibility = View.VISIBLE
            progressBar?.isIndeterminate = true

            // Call your existing publishClick logic here
            publishClick.onClick(btnPublish)
        }
        tvStatus = findViewById<View>(R.id.tvStatus) as TextView
        clientId = UUID.randomUUID().toString()
        val clientConfiguration = ClientConfiguration()
        credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            COGNITO_POOL_ID,
            MY_REGION
        )
        builder = AlertDialog.Builder(this@MainActivity)
        val region: Region = Region.getRegion(MY_REGION)
        mqttManager = AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT)
        mqttManager!!.setKeepAlive(10)
        val lwt = AWSIotMqttLastWillAndTestament(
            "my/lwt/topic",
            "Android client lost connection", AWSIotMqttQos.QOS0
        )
        mqttManager!!.setMqttLastWillAndTestament(lwt)
        mIotAndroidClient = AWSIotClient(credentialsProvider)
        mIotAndroidClient!!.setRegion(region)
        keystorePath = filesDir.path
        keystoreName = KEYSTORE_NAME
        keystorePassword = KEYSTORE_PASSWORD
        certificateId = CERTIFICATE_ID
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(
                        certificateId, keystorePath,
                        keystoreName, keystorePassword
                    )
                ) {
                    Log.i(
                        LOG_TAG, "Certificate " + certificateId
                                + " found in keystore - using for MQTT."
                    )
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(
                        certificateId,
                        keystorePath, keystoreName, keystorePassword
                    )
                } else {
                    Log.i(
                        LOG_TAG,
                        "Key/cert $certificateId not found in keystore."
                    )
                }
            } else {
                Log.i(
                    LOG_TAG,
                    "Keystore $keystorePath/$keystoreName not found."
                )
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e)
        }
        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.")
            Thread {
                try {
                    val createKeysAndCertificateRequest = CreateKeysAndCertificateRequest()
                    createKeysAndCertificateRequest.setSetAsActive(true)
                    val createKeysAndCertificateResult: CreateKeysAndCertificateResult
                    createKeysAndCertificateResult =
                        mIotAndroidClient!!.createKeysAndCertificate(createKeysAndCertificateRequest)
                    Log.i(
                        LOG_TAG,
                        "Cert ID: " +
                                createKeysAndCertificateResult.getCertificateId() +
                                " created."
                    )
                    AWSIotKeystoreHelper.saveCertificateAndPrivateKey(
                        certificateId,
                        createKeysAndCertificateResult.getCertificatePem(),
                        createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                        keystorePath, keystoreName, keystorePassword
                    )
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(
                        certificateId,
                        keystorePath, keystoreName, keystorePassword
                    )
                    val policyAttachRequest = AttachPrincipalPolicyRequest()
                    policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME)
                    policyAttachRequest.setPrincipal(
                        createKeysAndCertificateResult
                            .getCertificateArn()
                    )
                    mIotAndroidClient!!.attachPrincipalPolicy(policyAttachRequest)
                    runOnUiThread { connectAndSubscribe() }
                } catch (e: Exception) {
                    Log.e(
                        LOG_TAG,
                        "Exception occurred when generating new private key and certificate.",
                        e
                    )
                }
            }.start()
        } else {
            connectAndSubscribe()
        }
        xbeeNodesList.clear()
        updateListView()
    }

    private fun updateListView() {
        // Create an adapter for the ListView using the custom layout
        val adapter = object : ArrayAdapter<String>(this, R.layout.list_item_layout, R.id.textView1, xbeeNodesList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                val imageView5: ImageView = view.findViewById(R.id.imageView5)
                val imageView4: ImageView = view.findViewById(R.id.imageView4)
                val cardView: CardView = view.findViewById(R.id.cardView)

                // Set visibility based on position
                if (position == 0) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
                    imageView5.visibility = View.VISIBLE
                    imageView4.visibility = View.GONE
                } else if (position == count - 1) {
                    imageView5.visibility = View.GONE
                    imageView4.visibility = View.VISIBLE
                    cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
                } else {
                    imageView5.visibility = View.GONE
                    imageView4.visibility = View.GONE
                    cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                }

                return view
            }
        }

        // Set the adapter to the ListView
        listView?.adapter = adapter

        val itemCountTextView: TextView = findViewById(R.id.itemCountTextView)
        itemCountTextView.text = "Device Found ${xbeeNodesList.size}"
    }


    var publishClick = View.OnClickListener {
        val topic = PUBLISH_TOPIC_NAME
        val msg = txtMessage
        try {
            mqttManager?.publishString(msg, topic, AWSIotMqttQos.QOS0)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Publish error.", e)
        }
    }
    var disconnectClick = View.OnClickListener {
        try {
            mqttManager?.disconnect()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Disconnect error.", e)
        }
    }

    private fun connectAndSubscribe() {
        try {
            mqttManager?.connect(clientKeyStore, object : AWSIotMqttClientStatusCallback {
                override fun onStatusChanged(
                    status: AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus,
                    throwable: Throwable?
                ) {
                    Log.d(LOG_TAG, "Status = " + java.lang.String.valueOf(status))
                    runOnUiThread {
                        if (status === AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connecting) {
                            tvStatus!!.text = "Connecting..."
                        } else if (status === AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected) {
                            tvStatus!!.text = "Connected"
                            val topic = SUBSCRIBE_TOPIC_NAME
                            Log.d(
                                LOG_TAG,
                                "topic = $topic"
                            )
                            try {
                                mqttManager?.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                                    object : AWSIotMqttNewMessageCallback {
                                        override fun onMessageArrived(topic: String, data: ByteArray?) {
                                            runOnUiThread {
                                                try {
                                                    val message = data?.let {
                                                        try {
                                                            String(it, Charsets.UTF_8)
                                                        } catch (e: UnsupportedEncodingException) {
                                                            Log.e(LOG_TAG, "Message encoding error.", e)
                                                            null
                                                        }
                                                    }

                                                    message?.let {
                                                        Log.d(LOG_TAG, "Message arrived:")
                                                        Log.d(LOG_TAG, "   Topic: $topic")
                                                        Log.d(LOG_TAG, " Message: $it")
                                                        try {
//                                                            val json = JSONObject(it)
//                                                            val xbeeNodes = json.optString("XBee_Nodes", "")
//                                                            tvStatus1?.text = xbeeNodes
//                                                        } catch (e: JSONException) {
//                                                            Log.e(LOG_TAG, "JSON parsing error.", e)
//                                                        }
                                                            val json = JSONObject(it)
                                                            val xbeeNodes = json.optString("XBee_Nodes", "")

                                                            // Split XBee nodes based on semicolon
                                                            val nodesArray = xbeeNodes.split(";")

                                                            xbeeNodesList.clear()

                                                            // Process each node and format the details
                                                            for (node in nodesArray) {
                                                                val nodeDetails = node.split(",")
                                                                if (nodeDetails.size >= 4) {
                                                                    val deviceRole = when (nodeDetails[2].trim()) {
                                                                        "RO" -> "ROUTER"
                                                                        "CE" -> "COORDINATOR"
                                                                        else -> nodeDetails[2] // If neither "RO" nor "CO", keep the original value
                                                                    }
                                                                    val formattedNode =
                                                                                "MAC Address: 0013A200${nodeDetails[0]}\n" +
                                                                                "RSSI: ${nodeDetails[1]}\n" +
                                                                                "Device Role: $deviceRole\n" +
                                                                                "Node ID: ${nodeDetails[3]}"
                                                                    progressBar?.visibility = View.GONE

                                                                    xbeeNodesList.add(formattedNode)
                                                                }
                                                            }

                                                            // Update the ListView
                                                            updateListView()
                                                        } catch (e: JSONException) {
                                                            Log.e(LOG_TAG, "JSON parsing error.", e)
                                                        }

                                                    }
                                                } catch (e: UnsupportedEncodingException) {
                                                    Log.e(LOG_TAG, "Message encoding error.", e)
                                                }
                                            }
                                        }
                                    })
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_TAG,
                                    "Subscription error.",
                                    e
                                )
                            }
                        } else if (status === AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Reconnecting) {
                            if (throwable != null) {
                                Log.e(
                                    LOG_TAG,
                                    "Connection error.",
                                    throwable
                                )
                            }
                            tvStatus!!.text = "Reconnecting"
                        } else if (status === AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost) {
                            if (throwable != null) {
                                Log.e(
                                    LOG_TAG,
                                    "Connection error.",
                                    throwable
                                )
                            }
                            tvStatus!!.text = "Disconnected"
                        } else {
                            tvStatus!!.text = "Disconnected"
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Connection error.", e)
            tvStatus!!.text = "Error! " + e.message
        }
    }

    companion object {
        val LOG_TAG = MainActivity::class.java.canonicalName

        private const val CUSTOMER_SPECIFIC_ENDPOINT = "yourendpoint"
        private const val COGNITO_POOL_ID = "yourpoolid"
        private const val AWS_IOT_POLICY_NAME = "yourpolicy"
        private val MY_REGION: Regions = Regions.AP_SOUTH_1
        private const val KEYSTORE_NAME = "iot_keystore"
        private const val KEYSTORE_PASSWORD = "password"
        private const val CERTIFICATE_ID = "default"
        private const val PUBLISH_TOPIC_NAME = "XBee3/Command"
        private const val SUBSCRIBE_TOPIC_NAME = "XBee3/Data"
        private const val AUTHROLE_ARN = "XX"
        private const val UNAUTHROLE_ARN = "XX"
    }
}