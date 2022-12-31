# Manual tests to run before release

* Play a talk for at least an hour
  * Check audio quality (no skips)
  * Check that the talk doesn't get interrupted by Android killing the app
  * Check that rotating the screen doesn't stop the audio
  * Check bluetooth media buttons can be used to control the talk playback
  * Click the notification icon, make sure we go back to the play talk activity and that the back button goes back to the main navigation activity
  * Make sure the controls and UI work on the notification
  * Make sure we can close the notification and it stops the talk
  * Make sure the play talk UI components update correctly as we navigate away from and back to it
  
  
  
* Current problems
  * Play talk -> click notification -> back -> takes you to navigation activity but things like the starred state and search queries are erased
  * Can't delete notification
  * No player controls in notification
  * Blank icon in notification
  * Talk duration gets messed up often
  * Need to implement fragment to show currently playing talk while navigating elsewhere
