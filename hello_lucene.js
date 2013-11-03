
var http = require("http");
var url = require("url");
var fs = require("fs");	//file system
var path = require("path");	//路径处理
var querystring = require('querystring');

var server ;
var port = 6770;	//监听端口
var count = 0;	//总共处理数
var version= "0.0.6:20131103" ;

var jarPath ="/home/wuwenjie/lucene.jar";
var indexPath ="/media/linux_lenovo/L_index";

homePage="home.html";

var handle = {};
handle["/"] = home;
handle["/about"] = about;
handle["/random"]= random;
handle["/Shogun"]= Shogun;
handle["/search"]= search;
handle["/help"]= help;

var mimetypes = {
  "css"	: "text/css",
  "gif"	: "image/gif",
  "html": "text/html",
  "ico"	: "image/x-icon",
  "jpeg": "image/jpeg",
  "jpg"	: "image/jpeg",
  "js"	: "text/javascript",
  "json": "application/json",
  "pdf"	: "application/pdf",
  "png"	: "image/png",
  "svg"	: "image/svg+xml",
  "swf"	: "application/x-shockwave-flash",
  "tiff": "image/tiff",
  "txt"	: "text/plain",
  "wav"	: "audio/x-wav",
  "wma"	: "audio/x-ms-wma",
  "wmv"	: "video/x-ms-wmv",
  "xml"	: "text/xml"
};

function say(word) {
	
	var date = new Date();
	console.log("\x1B[33m------ ------\x1B[39m");
	console.log("\x1B[33m"+date.toUTCString()+":\n\t"+word+"\x1B[39m");
	//https://github.com/Marak/colors.js
	
}

function execute(someFunction, value) {
	someFunction(value);
}

var words = "Hello ! I was written by Nodejs on GNU/Linux !"
		+ "\n\tNow we on " + process.platform
		+ "\n\tNode Version: " + process.version
		+ "\n\tJS Version: " + version 
		+ "\n\tAuthor: wuwenjie.tk"

execute(say, words );

//-------------启动服务器--------------
function start(route, handle) {

  function onRequest(request, response) {
  
    var pathname = url.parse(request.url).pathname;
    var clientip= request.connection.remoteAddress;	//请求地址
    
    say("Request from "+clientip+" for " + pathname + " received.");

	count++;
	console.log("Processing No." + count + 
			" request(s)!\nUptime:" + process.uptime());

    //route(handle, pathname, response);
	//将response对象作为第三个参数传递给route()函数
	//将服务器响response应交由route函数处理
	
	//--------------POST--------------
	var postData = "";
	
	request.setEncoding("utf8");

    request.addListener("data", function(postDataChunk) {
      postData += postDataChunk;
      console.log("Received POST data chunk '"+postDataChunk + "'.");
    });

	//Class: http.ClientRequest
	request.on("end", function() {
		console.log("Request check end.");
		route(handle, request,pathname, response, postData);
    });
	//----------------
	
  }

  server=http.createServer(onRequest).listen(port);
  console.log("Server has started on " + port);
  
}

//服务器响应处理
function route(handle, request,pathname, response,postData) {
	
  console.log("Route : a request for " + pathname);
  
  var realPath = pathname.replace(/^\//g, "");	//替换，^\/
  
  if (typeof handle[pathname] === 'function') {	
	//如果handle[pathname]是函数
    handle[pathname](request,response,postData);
	//执行对应函数
  } 
  else if (fsExistSync(realPath)){	//路径是否存在
	
	console.log("file Exist '"+realPath+"'.");
	
		if (isDirSync(realPath)){	//是否为文件目录
	  
			console.log("Dir '"+realPath+"'.");
	  
			return200(response,realPath+" is Dir.");
	  
		}else{
			//向客户端传送数据
			writeFileToClient(request,response,realPath);
		}
  }
  else {
    throw404(response,pathname);
    //return301(response,"/");
  }
  
}

//--------200成功----------
function return200(response,str){
	console.log("Request OK !");
    response.writeHead(200, {"Content-Type": "text/plain"});
    response.write(""+str);
    response.end();
}

//--------301  Moved Permanently 永久移动------
function return301(response,moveto){
	
	console.log("Return 301 Moved Permanently !");
    response.writeHead(301, {"Location": moveto});
    response.write("301 重定向 Moved Permanently !");
    response.end();
	
}

//---------抛出404--------------
function throw404(response,pathname){
	
	console.log("throw404 for " + pathname);
    response.writeHead(404, {"Content-Type": "text/html"});
    response.write("<h1 style=\"text-align:center\">404 Not Found "
					+pathname+" !</h1>");
    response.end();
}

//---------500服务器内部错误-------
function throw500(response){

	response.writeHead(500, {'Content-Type': 'text/plain'});
	response.write("500 Internal Server Error \n"
					+"服务器遇到了一个未曾预料的状况，导致了无法完成对请求的处理!");
	response.end();
	//stackoverflow.com/questions/14835582/
	//nodejs-first-argument-must-be-a-string-or-buffer-when-using-response-write-w
	//throw new TypeError('first argument must be a string or Buffer');
}

//---------fsExistSync----------
function fsExistSync(realPath){	//Synchronous 同步

	console.log("File exist or not");
	
	var e=true;
	
	if(!fs.existsSync(realPath)){
		console.log("file Not Exist '"+realPath+"'.");
		e= false ;
	}
	
	return e;
}

//---------isDirSync------------
function isDirSync(qpath){
	
	//http://stackoverflow.com/questions/4482686
	//\/check-synchronously-if-file-directory-exists-in-node-js
	var stat = fs.statSync(qpath);
	
	return stat.isDirectory();
}

//向客户端传送文件
function writeFileToClient(request,response,realPath){

	console.log("Send File To Client '" + realPath+"'.");
	
	var fstat = fs.statSync(realPath);
	var ftotal = fstat.size/1024/1024;
	console.log("File '" + realPath+"' size is "+ftotal+" MB.");
	
	if(ftotal>100){	//如果文件大于100兆
	
		console.log("File '" + realPath+"' size larger than 100MB.");
		
		howtosendfile(request,response,realPath,"",1);
	
	}else{
	
		fs.readFile(realPath, "binary", function(err, file) {
			if (err) {
				throw500(response);
			} else {
				howtosendfile(request,response,realPath,file,0);
            }
      });
	  
	}
	  
}

//如何发送文件
function howtosendfile(request,response,realPath,file,large){

	console.log("How to send file !");

	//------------MIME 处理-------------
	var ext = path.extname(realPath);	//文件的后缀
	console.log("File  extname is '" + ext +"'.");
	
	ext = ext ? ext.slice(1) : 'unknown';
	//buf.slice([start], [end]) 裁 从第二个开始
	var contentType = mimetypes[ext];
	console.log("File MIME Types is '" + contentType +"'.");

	//如果在自定义MIME中，显示，不再，客户端下载
	if(!(contentType === undefined )){	//全等（值和类型）
	
		if(large==0){
			console.log("Send small File in MIME types '" + realPath+"'.");
			response.writeHead(200, {'Content-Type': contentType,});
			response.write( file, "binary");
			response.end();
		}
		else{
			console.log("Send File '" + realPath+"' use stream.");
			//http://stackoverflow.com/questions/6926721/event-loop-for-large-files
			var stream = fs.createReadStream(realPath, { bufferSize: 64 * 1024 });
			stream.pipe(response);
		}
		
	}else if ( ext == "mp4"|ext == "webm" ){	
		//https://gist.github.com/paolorossi/1993068
	
		var stat = fs.statSync(realPath);
		var total = stat.size;
		/*
		* 206 Partial Content 服务器已经成功处理了部分GET请求
		* 使用此类响应实现[断点续传]或者将一个大文档分解为多个下载段同时下载
		*/
		if (request.headers['range']) {
			var range = request.headers.range;
			var parts = range.replace(/bytes=/, "").split("-");
			var partialstart = parts[0];
			var partialend = parts[1];

			var start = parseInt(partialstart, 10);
			var end = partialend ? parseInt(partialend, 10) : total-1;
			var chunksize = (end-start)+1;
			console.log('RANGE: ' + start + ' - ' + end + ' = ' + chunksize);

			var file = fs.createReadStream(realPath, {start: start, end: end});
			response.writeHead(206, 
				{ 'Content-Range': 'bytes ' + start + '-' + end + '/' + total, 
					'Accept-Ranges': 'bytes', 'Content-Length': chunksize, 
					'Content-Type': 'video/'+ext });
			file.pipe(response);
		} else {
			console.log('ALL: ' + total);
			response.writeHead(200, { 'Content-Length': total,
						'Content-Type': 'video/'+ext });
			fs.createReadStream(realPath).pipe(response);
			}
	}	//如果是mp4
	else{
		console.log("Send File " + large + " '" + realPath+"' Out of MIME types.");
		
		if(large==0){
			response.writeHead(200, {"Content-Disposition": "attachment;"+realPath});
			response.write(file, "binary");
			response.end();
		}else{
			console.log("Send File '" + realPath+"' use stream.");
			var stream = fs.createReadStream(realPath, { bufferSize: 64 * 1024 });
			response.writeHead(200, {"Content-Disposition": "attachment;"+realPath});
			stream.pipe(response);
		}
		
	}

}

//-----requestHandler----------------

function home(request,response,postData) {

	//homePage="home.html";

  console.log("Request handler 'home' was called.");
  
  if(fsExistSync(homePage)){
	writeFileToClient(request,response,homePage);
  }else{
	  throw404(response,homePage);
	  console.log("Request handler 'home' homePage not found.");
  }
}

/*
//载入模块
var querystring = require('querystring');
var url = require("url");

                               url.parse(string).query
                                           |
           url.parse(string).pathname      |
                       |                   |
                       |                   |
                     ------ -------------------
http://localhost:6770/start?foo=bar&hello=world
                                ---       -----
                                 |          |
                                 |          |
              querystring(string)["foo"]    |
                                            |
                         querystring(string)["hello"]
*/

//查找
function search(request,response,postData){
	
	console.log("Request handler 'search' was called.");
	var ReUrl = request.url ;	//请求的完整链接
	console.log("\t'search' request url is "+ReUrl);
	
	var query = url.parse(ReUrl).query;	//请求的参数集合
	console.log("\t'search' request query is "+query);
	
	var queryStr = querystring.parse(query)["q"];	//请求参数q的值
	console.log("\t'search' query sring is "+queryStr);
	
	queryStr=queryStr.replace(/"/g, "");	//替换，"
	
	//if search query is null then redirect to home.html
	if (queryStr==''){
		return301(response,homePage);
	}else{
	
	//执行本地命令
	var shell_name = "java -server -Xms1024m -Xmx1024m -jar "+ jarPath + " -SQPS " 
					+ indexPath + " \"" + queryStr +"*\" name "+"\"\";";
					
	var shell_contents = "java -server -Xms1024m -Xmx1024m -jar "+ jarPath + " -SQPS " 
					+ indexPath + " \"" + queryStr +"\" contents "+"\"\";";
					
	var shell_all = shell_name + "\n" + shell_contents;
	
	console.log("\t'search' query shell_name is \n\t\t"+ shell_all +".");
	
	console.time('exec search');	//记时
	execShellCommand(shell_all,response);	//使用本地shell执行命令并返回请求
	console.timeEnd('exec search');
	}
} 

//------执行命令-----------
var exec = require("child_process").exec;
//实现一个既简单又实用的非阻塞操作：exec()
function execShellCommand(shell,response){
	
	//------执行命令-----------
	exec( shell , { timeout: 10000, maxBuffer: 20000*1024 },
	function (error, stdout, stderr) {

		if(stderr){
			console.error("\t'search' query shell has problem\n\t\t*** "+ stderr);
			throw500(response);
		}else{
			return200(response,stdout);
		}
  });
}

function about(request,response,postData) {

  console.log("Request handler 'about' was called.");
  response.writeHead(200, {"Content-Type": "text/plain"});
  response.write(words
			+"\n\nNode.js is a platform built on Chrome's JavaScript runtime \n" 
			+ "for easily building fast,scalable network applications.\n" 
			+ "\nNode.js uses an event-driven, non-blocking I/O model \n"
			+ "that makes it lightweight and efficient, \n" 
			+ "perfect for data-intensive real-time applications \n" 
			+ "that run across distributed devices.");
  response.end();
}

function random (request,response,postData){

	console.log("Request handler 'random' was called.");

	var rnum=Math.random();
	
	response.writeHead(200, {"Content-Type": "text/plain"});
	response.write("random: " +rnum);
	response.end();
}

function Shogun (request,response){

	console.log("Request handler 'Shogun' was called.");

	var query = url.parse(request.url).query;
	response.writeHead(200, {"Content-Type": "text/plain"});
	var querystring = require('querystring');	//载入模块module
    response.write("your request is "
			+ querystring.parse(query)["q"]+" &上杉謙信");
    response.end();
}

function help (request,response){
	
	console.log("Request handler 'help' was called.");
	response.writeHead(200, {"Content-Type": "text/plain"});
	
    response.write("rbenv ps	rbenv OR ps\n"+
		"rbenv AND rake		+rbenv+rake\n"
		+"title:ant		title域包含ant项的文档 contents:pwd\n"
		+"(contents:pwd or cat) AND file	内容包括file并且(包括pwd或cat之一)\n"
		+"\"rake octopress\"~5	rake octopress 之间距离小于5的文档 被过滤，不可使用");
    response.end();

}

process.stdin.resume();
process.stdin.setEncoding('utf8');

process.stdin.on('data', function(chunk) {
	process.stdout.write('data: ' + chunk);
});

process.stdin.on('end', function() {
  process.stdout.write('end.\n');
  process.exit();
});

process.on('exit', function() {
  console.log('About to exit.');
});

process.on('uncaughtException', function(err) {
  console.log('Caught exception: ' + err);
});

process.on('SIGINT', function() {
  console.log('Got SIGINT.  Press Control-D to exit.');
});


start(route, handle);
//启动服务器