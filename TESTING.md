# Manual tests to run before release

* Play a talk for at least an hour
  * Check audio quality (no skips)
  * Check that the talk doesn't get interrupted by Android killing the app
  * Check that rotating the screen doesn't stop the audio
  * Check bluetooth media buttons can be used to control the talk playback
  * Click the notification icon, make sure we go back to the play talk activity and that the back button goes back to the main navigation activity
  * Make sure the controls and UI work on the notification
  * Make sure the play talk UI components update correctly as we navigate away from and back to it
  * Pause and resume the talk from the notification and the UI and make sure the service is running every whenever the talk is playing, as noted in the "X apps are active" info of the pulldown settings
* Test using https://github.com/googlesamples/android-media-controller  
* Test on very old and very new devices using the emulator 
  

