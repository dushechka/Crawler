=============================

Crawler  
Version 0.7.7  
July 05, 2018  
Created by Andrey Fadeev  
License: Apache v2.0  

=============================

This program scans given websites that you will specify, saves   
vocabularies from articles on them in Redis DB and fetches the amount of  
keywords occurrences for each person that you specify in the MySQL database.
  
  
Installation:  

1) You'll need to create MySQL or MariaDB database, using script  
"DB_0.0.5.3.sql" in "resources/db/" folder;  

2) You will need to add sites and persons in db or use script  
"DB_0.0.5.2_data.sql" in "resources/db/" folder. It will add  
some data. Notice: You have to add at least one page for each  
website in "PAGES" table. Otherwise the program won't crawl them;  

3) Specify your Redis and MySQL properites in corresponding files  
in "src/main/resources/" folder;  

4) Run maven install task: mvn install;  

5) Now you can run "crawler-0.7.7-jar-with-dependencies" jar file.  
Launched with no parameters, it will show you the usage.  
