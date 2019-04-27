# ArduinoController
Android Application for sent and recieve data from Arduino.

## Build and Run Application
Clone this repo and open with Android Studio.

## Arduino Code:

```c
void setup()
{  
 // Open serial communications and wait for port to open:
  Serial.begin(9600);
}

void loop()
{
  while(Serial.available() > 0) {
    Serial.write(Serial.read());
  }
}

```
## Screenshots
![Screenshot 1](https://github.com/Anooppandikashala/ArduinoController/blob/master/ScreenshotOne.png)
