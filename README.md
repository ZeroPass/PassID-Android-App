# PassID-Android-App
Android app client for PassID PoC.

# Usage
  Before running the app [PassID server](https://github.com/ZeroPass/PassID-Server) should be up and running.

  1. Open project in Android Studion
  2. Run the app
  4. In the app go to settings and set server's URL address
  5. Close settings and go to `Register` screen
  6. Scan data with camera or fill in data from passport
     e.g.: Passport number, date of birth and date of expiry
  7. Press `Scan` button and put your passport near your android phone.
  8. If scan completes successfully and there is no communication error with the server,  
     you should end up on the `Success` screen 
  9. Go back and open `Login` screen
  10. Fill in data into form as in step 5 then follow step 6
  11. After successful login you can try to login again.
      This time the server should request your personal data from passport (EF.DG1 file).
      If you choose to send this file to the server it will return greeting with your name.
