# Robot Car Controller - Android App

This is an Android application for controlling a robot car via WebSocket. It provides multiple control modes and supports dynamic IP/port configuration to connect to different robot car instances running on ROS 2.

## 🚗 Features

- **Manual Control Mode**  
  Drive the robot car manually using virtual joystick controls or touch gestures.

- **Auto Navigation Mode**  
  Tap on a map or interface to set a navigation goal. The robot will autonomously move to the selected destination using ROS 2 Nav2.

- **Tracker Mode**  
  Enable object or line tracking mode, where the robot follows a target based on camera input and vision processing.

- **Settings Panel**  
  - Set or update the IP address and port to connect with the robot car's WebSocket server.
  - Save and apply connection settings on the fly.

## ⚙️ Requirements

- Android 7.0 (API 24) or above
- Robot Car system running ROS 2 with WebSocket server exposed

## 🔌 Connection

1. Open the app and go to the **Settings** page.
2. Enter the IP address and port of the robot car's WebSocket server.
3. Save the settings and return to the home screen.
4. Choose a mode to begin controlling the robot.

## 🧪 Modes in Detail

### 1. Manual Mode
- Virtual joystick and button interface
- Direct control of movement and direction of the camera
- Displays the camera view
- Diplays cartographic map with laser scans around the robot

### 2. Auto Navigation Mode
- Sends goal poses to `/navigate_to_pose` ROS 2 action
- Displays live feedback and navigation status

### 3. Tracker Mode
- Activates camera-based tracking of the hands or line follower
- Sends commands to follow target in real-time
- Displays status of the tracker

## 📷 Screenshots

![image](https://github.com/user-attachments/assets/eb930d42-49e6-4739-919a-bea486f95fec)
![image](https://github.com/user-attachments/assets/2b0a8740-bb77-4b46-8c89-7b5f59efdbea)
![image](https://github.com/user-attachments/assets/058f8f5b-999d-4eb3-b1af-77d1da26f82a)


## 🤝 Contributing

Feel free to fork the repository and submit pull requests. Suggestions and improvements are welcome!

## 📄 License

MIT License

