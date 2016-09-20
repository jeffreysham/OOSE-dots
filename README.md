# OOSE-dots
Jeffrey Sham, jsham2

CS 421

This is assignment 1 for OOSE Fall 2016. It is the classic dots game (https://www.math.ucla.edu/~tom/Games/dots&boxes.html).
This game is run on localhost:8080 and stores the data in a SQLite database.

Assignment information: http://pl.cs.jhu.edu/oose/assignments/assignment1.shtml

I ran this project by using eclipse. The included pom file should be able to build the necessary dependencies.
The Junit tests are in the src/test/... folder (it it called TestDotsServer.java).

I was able to run the code with the nice front end and it worked as intended. I created 4 different database
tables in order to complete the assignment. I was thinking about condensing it down to just 3 tables (moves,
player, and games) but I did not want to recreate the board in order to find out if a box was won. I also
do not use the Game object and Player object to store the data for extended periods of time because I wanted
the server to be able to handle multiple games. I understand I could have created a map of games and held
the information of each game in the map, but I thought of that after I already started and I also wanted
to try to make full use of the database.

For my errors, in addition to the error status code, I return json explaining the error. Also, I had a lot
of error checking just in case there was bad input.

Enjoy :)
