# AWS IoT Android Application

This Android application allows users to connect to AWS IoT services, manage devices, and publish/subscribe to MQTT topics. The app uses AWS IoT SDK for Android and integrates AWS Cognito for user authentication.

## Features

- Connects to AWS IoT using MQTT.
- Publishes commands to IoT devices via MQTT topics.
- Subscribes to MQTT topics and processes incoming messages.
- Supports AWS Cognito for credentials management.
- Displays connected device status and information in real-time.

## Prerequisites

Before you start, make sure you have the following:

- Android Studio installed on your machine.
- An AWS account.
- AWS IoT setup (including creating things, certificates, and policies).
- AWS Cognito User Pool and Identity Pool setup.

## Setup and Installation

### 1. Clone the repository

Clone this repository to your local machine using the following command:

git clone https://github.com/your-username/aws-iot-android-app.git
### 2. Open the project in Android Studio
Launch Android Studio.
Open the cloned project.

### 3. Configure AWS Credentials
You need to set up AWS credentials and IoT configurations for the application.
Replace the placeholders in the code with your own AWS IoT configuration:
COGNITO_POOL_ID: Your AWS Cognito Identity Pool ID.
MY_REGION: Your AWS region (e.g., Regions.US_EAST_1).
CUSTOMER_SPECIFIC_ENDPOINT: Your AWS IoT endpoint (e.g., your-endpoint.iot.us-east-1.amazonaws.com).
KEYSTORE_NAME: The name of the keystore used to store your device's private key and certificate.
KEYSTORE_PASSWORD: The password for your keystore.
You can find the IoT endpoint in the AWS IoT Console.
Set up AWS IoT policies for your devices (attach appropriate policies for MQTT access).

### 4. Build and Run the App
Connect an Android device or use an emulator.

Click Run in Android Studio to build and deploy the app on your device.

### 5. AWS IoT Setup
AWS IoT Policy: Ensure that you attach the appropriate policies to allow the Android application to interact with AWS IoT.

Certificates: The app uses AWS IoT certificates to authenticate and encrypt the connection. Ensure that your device's certificates are correctly set up and stored in the app's keystore.

### 6. Key Components in the App
AWSIotMqttManager: Manages MQTT connections, topics, and messages.
AWSIotClient: Communicates with the AWS IoT service to create certificates and policies.
CognitoCachingCredentialsProvider: Handles authentication using AWS Cognito.
ListView: Displays the status of connected IoT devices.

### 7. Running the Application
When the app is run:
It attempts to connect to the AWS IoT endpoint using MQTT.
It subscribes to a predefined topic (e.g., my/topic).
It can publish messages to the IoT devices with commands (e.g., scan devices).
It displays device status (e.g., Connected, Disconnected) in a list.

## Troubleshooting
Connection Issues: Ensure your AWS IoT endpoint is correctly configured, and the device certificates are present in the keystore.
Keystore Errors: If the app cannot find the certificate in the keystore, it will create a new certificate and key.
Log Errors: Check the logcat for detailed error messages related to AWS IoT and MQTT connections.


### ⭐️ Show your support

If you found this project useful, please give it a ⭐️ on GitHub and consider sharing it with others

## 🧑‍💻 Author

**Mohammed Sulaiman**  
SOFTWARE, AI & Data Engineering Enthusiast  
📫 mdsulaiman5134@gmail.com(mailto:mdsulaiman5134@gmail.com)  
🌐 [LinkedIn](https://www.linkedin.com/in/mohammed-sulaiman-23a10021b)
