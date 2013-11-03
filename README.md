hello_luence
============

a simple native search engine power by luence and nodejs

##How To Use
 * you need `java` & `nodejs`
  * java : [how to install](http://openjdk.java.net/install/)
  * nodejs : [how to install](https://github.com/joyent/node/blob/master/README.md)
  * In debian, you can follow this [link](https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-mint-elementary-os)
 * Compile LuceneSearch
  * export as a Runnable JAR file (include required libs)
 * Run hello_lucene.js
  * `nodejs hello_lucene.js`
  * open the browser as [`http://127.0.0.1:6770/`](http://127.0.0.1:6770/)

##ChangeLog
 * 20131006 
  * first init
 * 20131103
  * hello_lucene.js :
  * improve large file (more than 100MB) transfer
  * if search query is null then redirect to home.html
  * LuceneSearch :
  * add New index method AI2 (unstable)

##Author
 * [wuwenjie1992](http://wuwenjie.tk)
 
##Thank For
 * [`luence`](http://lucene.apache.org/)
 * [`nodejs`](http://nodejs.org)
 * java
 * Eclipse
 * Xubuntu
 * GNU/Linux
