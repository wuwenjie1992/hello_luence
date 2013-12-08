package tk.wuwenie.luceneSearch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * @author wuwenjie
 * 
 * @import lucene-core-4.5.0.jar;lucene-analyzers-common-4.5.0.jar
 * 
 * @demo lucene-4.5.1/docs/demo/src-html/org/apache/lucene/demo/IndexFiles.html
 * 
 */
public class TxtFileIndexer {

	static File dataDir = null; // 需要被建立索引的文件目录
	static File indexDir = null; // 存放索引的目录
	File indexDirCN = null; // 存放中文索引的目录
	static ArrayList<File> Compliance_f = new ArrayList<File>();// 符合要求的文件
	static FSDirectory index_dir = null; // FSDirectory表示一个存储在文件系统中的索引的位置
	static FSDirectory index_dir_cn = null;
	RAMDirectory index_ram_dir = null; // 存储在内存当中的索引的位置,作为缓冲区改善性能

	Analyzer luceneAnalyzer = null; // 分析器
	IndexWriter indexWriter = null; // 索引写入器
	IndexWriter indexWriter_ram = null; // 【内存索引写入器】

	static Analyzer chineseAnalyzer = null; // 中文分析器
	static IndexWriter indexWriter_cn = null; // 线程共享的索引书写器
	static NIOFSDirectory nioD = null; // 使用NIP的目录对象
	static IndexWriterConfig iwc = null; // 【索引书写器】的设置

	// 调用makeIndex初始化必须执行
	public TxtFileIndexer(String dataDir_s, String indexDir_s) throws Exception {

		dataDir = new File(dataDir_s);

		indexDir = new File(indexDir_s);
		index_dir = FSDirectory.open(indexDir);// (indexDir,null);
		// public static FSDirectory open(File path)throws IOException
		// Creates an FSDirectory instance 创建一个FSDirectory实例

		indexDirCN = new File(indexDir_s + File.separator + "cn");
		index_dir_cn = FSDirectory.open(indexDirCN);

		// -----------初始化第二步：【分析器】和【索引写入器】---------------------
		luceneAnalyzer = new StandardAnalyzer(Version.LUCENE_45);
		// 在文档被索引前，先要对文档内容进行分词处理，由 Analyzer 来做的
		// Analyzer 类是一个抽象类，它有多个实现。针对不同的语言和应用需选择适合的 Analyzer
		// Analyzer 把分词后的内容交给 IndexWriter 来建立索引

		// StandardAnalyzer 标准分析
		// public StandardAnalyzer(Version matchVersion)
		// Builds an analyzer with the default stop words (STOP_WORDS_SET).
		// Parameters:matchVersion - Lucene version to match See above
		indexWriter = new IndexWriter(index_dir, new IndexWriterConfig(
				Version.LUCENE_45, luceneAnalyzer));

		chineseAnalyzer = new CJKAnalyzer(Version.LUCENE_45);// 中文分析器
		indexWriter_cn = new IndexWriter(index_dir_cn, new IndexWriterConfig(
				Version.LUCENE_45, chineseAnalyzer));
	}

	// 分析、创建索引
	public void makeIndex(boolean UseRAM, boolean showToken) throws Exception {

		ArrayList<File> daf = findCompliantFile(dataDir, "(.*\\.txt)");// 符合要求的文件
		int egn = getGroupSize(daf.size());

		long startTime = new Date().getTime();

		// ---------------建立索引--------------------
		for (int i = 0; i < daf.size(); i++) {

			System.out.println("Indexing " + i + ":"
					+ daf.get(i).getCanonicalPath());

			Document document = new Document();
			// Document 是用来描述文档的，文档可指HTML页面，电子邮件，或者文本文件。
			// 一个 Document 对象由多个 Field 对象组成的。
			// 可把Document对象想象成数据库中的一个记录，而每个 Field 对象就是记录的一个字段

			Reader txtReader = new FileReader(daf.get(i));

			Field path_fd = new StringField("path", daf.get(i)
					.getCanonicalPath(), Field.Store.YES);
			// Field对象是描述文档的某属性的，
			// 比如电子邮件的标题和内容可用两个Field对象分别描述
			document.add(path_fd);

			Field modified_fd = new LongField("modified", daf.get(i)
					.lastModified(), Field.Store.YES);
			document.add(modified_fd);

			Field contents_fd = new TextField("contents", txtReader);
			document.add(contents_fd);

			contents_fd.setBoost(3.0f);
			// 设置域增强因子重要级别
			// System.out.println("contents_fd:"+contents_fd.boost());

			if (UseRAM == true) { // 是否将Index先存在内存中
				System.out.println("UseRAM---indexWriter_ram");

				index_ram_dir = new RAMDirectory(); // 构造【空内存目录】
				indexWriter_ram = new IndexWriter(
						index_ram_dir,
						new IndexWriterConfig(Version.LUCENE_45, luceneAnalyzer));
				// 实例化【内存索引写入器】

				indexWriter_ram.addDocument(document);
			}

			else {
				indexWriter.addDocument(document);
				// 把一个个的 Document 对象加到索引中来
			}

			if (UseRAM && i > 0 && i % egn == 0) { // 每处理egn个时合并内存索引

				indexWriter_ram.close();// 关闭【内存索引器】
				// no segments* file found in
				// org.apache.lucene.store.RAMDirectory
				indexWriter.addIndexes(new Directory[] { index_ram_dir });
				// 合并索引，将【内存中的索引】通过【indexWriter】写到磁盘

				index_ram_dir.close();
				index_ram_dir = new RAMDirectory(); // 构造一个空目录

				indexWriter_ram = new IndexWriter(
						index_ram_dir,
						new IndexWriterConfig(Version.LUCENE_45, luceneAnalyzer));
			}

		}

		indexWriter.forceMerge(50);
		// 强制合并策略:合并段直到有<= maxNumSegments
		// indexWriter.optimize();
		indexWriter.close();

		// ----------------------------------------------
		// ---------------建立中文索引--------------------
		// -----------------------------------------------
		for (int i = 0; i < daf.size(); i++) {

			System.out.println("cn Indexing " + i + ":"
					+ daf.get(i).getCanonicalPath());

			Document document_cn = new Document();
			Reader txtReader = new FileReader(daf.get(i));
			document_cn.add(new StringField("path", daf.get(i)
					.getCanonicalPath(), Field.Store.YES));
			document_cn.add(new LongField("modified",
					daf.get(i).lastModified(), Field.Store.NO));
			document_cn.add(new TextField("contents", txtReader));

			indexWriter_cn.addDocument(document_cn);

			// 分词器-------TokenStream处理枚举标记的序列，无论是从文档字段或查询文本----------
			if (showToken) {
				TokenStream stream2 = chineseAnalyzer.tokenStream("contents",
						new FileReader(daf.get(i)));
				/**
				 * 新建一个reader java.io.IOException: Stream closed at
				 * sun.nio.cs.StreamDecoder.ensureOpen(StreamDecoder.java:46) at
				 * sun.nio.cs.StreamDecoder.read(StreamDecoder.java:147) at
				 * java.io.InputStreamReader.read(InputStreamReader.java:184) at
				 * org. apache.lucene.analysis.standard.StandardTokenizerImpl.
				 * zzRefill (StandardTokenizerImpl.java:923)。。。
				 **/

				String s2 = "";
				CharTermAttribute ta2 = stream2
						.getAttribute(CharTermAttribute.class);
				OffsetAttribute offAtt = stream2
						.addAttribute(OffsetAttribute.class);

				try {
					stream2.reset(); // removes NullPointerException
					// stackoverflow.com/questions/16758271/simple-test-code-for-lucene-4-3-0-dont-work
					// stackoverflow.com/questions/1675739/to-split-only-chinese-characters-in-java
					while (stream2.incrementToken()) {
						s2 += " {" + ta2.toString() + offAtt.startOffset()
								+ " " + offAtt.endOffset() + "} ";
					}
					System.out.println(s2);
					stream2.end();
				} finally {
					stream2.close();
					// Release resources associated with this stream.
				}
			}// if
				// --------------TokenStream----------------

		}

		indexWriter_cn.forceMerge(50);// 强制合并策略
		indexWriter_cn.close();

		long endTime = new Date().getTime();
		System.out.println("\nIt takes " + (endTime - startTime)
				+ " milliseconds to create index for the files in directories "
				+ dataDir.getPath() + "\n");
	}// makeIndex

	// indexWriter.deleteDocuments(term); 删除索引中的文档，更新为先删再加,

	// ---------------------------------------------------------------------
	// --------调用makeIndexAdvance，WriteDocThread初始化必须执行--------------
	public static void IndexerInAdvance(String indexDir_s) throws Exception {

		// --------indexWriter初始化---------------
		chineseAnalyzer = new CJKAnalyzer(Version.LUCENE_45);
		// 中文分析器，可以使用其他，庖丁解牛分词器 code.google.com/p/paoding/

		indexDir = new File(indexDir_s);
		nioD = new NIOFSDirectory(indexDir);

		iwc = new IndexWriterConfig(Version.LUCENE_45, chineseAnalyzer);

		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		// Creates a new index if one does not exist
		// otherwise it opens the index and documents will be appended.

		iwc.setRAMBufferSizeMB(1024);// 内存上限
		IndexWriterConfig.setDefaultWriteLockTimeout(10);

		// http://space.itpub.net/28624388/viewspace-766134

	}

	// -------当文件很多时多线程效果显著----------
	public static void makeIndexAdvance(String dataDir_s) throws Exception {

		File data_f = new File(dataDir_s);

		ArrayList<File> daf = findCompliantFile(data_f,
				"(.*\\.txt|.*\\.html|.*\\.HTM|.*\\.htm|.*\\.HTML|*\\.shtml)");
		// 符合要求的文件 (.*\.txt|.*\.html|.*\.HTM|.*\.htm|.*\.HTML)

		// ---------分组处理------------------
		int fsize = daf.size();
		int gs = getGroupSize(fsize);
		int group = fsize / gs;

		System.out.println("All file :" + fsize + " Group:" + group
				+ " Group Size:" + gs + " Processors:"
				+ Runtime.getRuntime().availableProcessors() + "\n");

		// -----IndexWriter设置------------
		iwc.setMaxThreadStates(group + 2);
		if ((fsize >> 2) >= 2)
			iwc.setMaxBufferedDocs(fsize >> 2);

		System.out.println("\nRAMBufferSizeMB:" + iwc.getRAMBufferSizeMB()
				+ "\nDefaultWriteLockTimeout:"
				+ IndexWriterConfig.getDefaultWriteLockTimeout()
				+ "\nOpenMode:" + iwc.getOpenMode() + "\nIndexDeletionPolicy:"
				+ iwc.getIndexDeletionPolicy() + "\nWriteLockTimeout:"
				+ iwc.getWriteLockTimeout() + "\nMergePolicy:"
				+ iwc.getMergePolicy() + "\nMaxThreadStates:"
				+ iwc.getMaxThreadStates() + "\nReaderPooling:"
				+ iwc.getReaderPooling() + "\nRAMPerThreadHardLimitMB:"
				+ iwc.getRAMPerThreadHardLimitMB()
				+ "\nMaxBufferedDeleteTerms:" + iwc.getMaxBufferedDeleteTerms()
				+ "\nMaxBufferedDocs:" + iwc.getMaxBufferedDocs()
				+ "\nReaderTermsIndexDivisor:"
				+ iwc.getReaderTermsIndexDivisor() + "\nTermIndexInterval:"
				+ iwc.getReaderTermsIndexDivisor() + "\n");

		indexWriter_cn = new IndexWriter(nioD, iwc); // 重用已定义的IndexWriter
		// 一个IndexWriter对象可以被多个线程所共享

		// --------------WriteDocThread----------------------
		for (int i = 0; i < group; i++) {

			List<File> sub_l = daf.subList(i * gs, (i + 1) * gs);
			// 按分组结果，进行分配【处理文件队列】

			WriteDocThread wdt = new WriteDocThread("t" + i, sub_l, false);
			// No enclosing instance of type TxtFileIndexer is accessible.
			// Must qualify the allocation with an enclosing instance of type
			// TxtFileIndexer
			// (e.g. x.new A() where x is an instance of TxtFileIndexer).
			// - The value of the local variable wdt is not used
			// blog.csdn.net/sunny2038/article/details/6926079

			wdt.start();

		}

		// 分组结果可能会少计入【待处理文件】
		int Missing = fsize - (gs * group);

		if (Missing > 0) {

			WriteDocThread wdts = new WriteDocThread("t_f", daf.subList(fsize
					- Missing, fsize), false);
			System.out.println("\nMissing:"
					+ daf.subList(fsize - Missing, fsize).size());
			wdts.start();
		}

		// new Thread() {public void run() {}//run
		// };//Thread

		// IndexWriter.isLocked(nioD);

		// addIndexes(Directory... dirs)
		// Adds all segments from an array of indexes into this index.

		// updateDocument(Term term, Iterable<? extends IndexableField> doc)
		// Updates a document by first deleting the document(s) containing term
		// and then adding the new document.

	}

	public static void makeIndexAdvance2(String dataDir_s) throws Exception {

		File data_f = new File(dataDir_s);

		ArrayList<File> daf = findCompliantFile(data_f,
				"(.*\\.txt|.*\\.html|.*\\.HTM|.*\\.htm|.*\\.HTML|.*\\.shtml)");
		// 符合要求的文件 (.*\.txt|.*\.html|.*\.HTM|.*\.htm|.*\.HTML)

		// ---------分组处理------------------
		int fsize = daf.size();
		int gs = getGroupSize(fsize);
		int group = fsize / gs;
		int big_group = group >> 2;
		int big_gs = fsize / big_group;

		System.out.println("All file :" + fsize + " Big Group:" + big_group
				+ " big_gs:" + big_gs + " Processors:"
				+ Runtime.getRuntime().availableProcessors() + "\n");

		// -----IndexWriter设置------------
		iwc.setMaxThreadStates(group + 2);
		if ((fsize >> 2) >= 2)
			iwc.setMaxBufferedDocs(fsize >> 2);

		System.out.println("\nRAMBufferSizeMB:" + iwc.getRAMBufferSizeMB()
				+ "\nDefaultWriteLockTimeout:"
				+ IndexWriterConfig.getDefaultWriteLockTimeout()
				+ "\nOpenMode:" + iwc.getOpenMode() + "\nIndexDeletionPolicy:"
				+ iwc.getIndexDeletionPolicy() + "\nWriteLockTimeout:"
				+ iwc.getWriteLockTimeout() + "\nMergePolicy:"
				+ iwc.getMergePolicy() + "\nMaxThreadStates:"
				+ iwc.getMaxThreadStates() + "\nReaderPooling:"
				+ iwc.getReaderPooling() + "\nRAMPerThreadHardLimitMB:"
				+ iwc.getRAMPerThreadHardLimitMB()
				+ "\nMaxBufferedDeleteTerms:" + iwc.getMaxBufferedDeleteTerms()
				+ "\nMaxBufferedDocs:" + iwc.getMaxBufferedDocs()
				+ "\nReaderTermsIndexDivisor:"
				+ iwc.getReaderTermsIndexDivisor() + "\nTermIndexInterval:"
				+ iwc.getReaderTermsIndexDivisor() + "\n");

		indexWriter_cn = new IndexWriter(nioD, iwc); // 重用已定义的IndexWriter
		// 一个IndexWriter对象可以被多个线程所共享

		// --------------WriteDocThread----------------------

		int in_big_gs = getGroupSize(fsize / big_group) * 3; // 组大小,3464
		int in_big_gn = fsize / big_group / in_big_gs; // 组数,3

		System.out
				.println("in_big_gs:" + in_big_gs + " in_big_gn:" + in_big_gn);

		for (int i = 0; i < big_group; i++) { // 4

			WriteDocThread wdt = null;

			for (int j = 0; j < in_big_gn; j++) { // 3

				int beg = i * big_gs + j * in_big_gs;
				int end = i * big_gs + (j + 1) * in_big_gs;

				System.out.println(i + ":" + j + " " + beg + ":" + end);

				List<File> sub_l = daf.subList(beg, end);
				// 按分组结果，进行分配【处理文件队列】

				wdt = new WriteDocThread("t" + i + ":" + j, sub_l, false);

				wdt.start();

			}

			System.out.println(i * big_gs + in_big_gs * in_big_gn + ":"
					+ (i + 1) * big_gs);

			WriteDocThread wdts = new WriteDocThread("t_f", daf.subList(i
					* big_gs + in_big_gs * in_big_gn, (i + 1) * big_gs), false);

			wdts.run();

			wdt.join();

		}

		// 分组结果可能会少计入【待处理文件】
		int Missingbegin = big_group * big_gs;

		if (Missingbegin < fsize) {

			WriteDocThread wdts = new WriteDocThread("t_f", daf.subList(
					Missingbegin, fsize), false);
			System.out.println("\nMissing:" + Missingbegin + ","
					+ daf.subList(Missingbegin, fsize).size());
			wdts.start();
		}

	}

	// IndexWriter addDocument线程 使用前调用IndexerInAdvance
	public static class WriteDocThread extends Thread {

		List<File> subf;
		int subf_l;
		int CommitSpace; // 提交间隔
		boolean UpdateOrNot;
		Document doc_cn;

		public WriteDocThread(String name, List<File> f, boolean Update) {
			super(name);
			this.subf = f;
			subf_l = subf.size();
			CommitSpace = getGroupSize(subf_l) * 5; // 提交间隔
			// System.out.println("subf_l:" + subf_l + " CommitSpace:"
			// + CommitSpace);
			UpdateOrNot = Update;
		}

		@Override
		public void run() {

			// this.setPriority(Thread.NORM_PRIORITY);

			// ---------------建立索引--------------------
			int i;
			for (i = 1; i <= subf_l; i++) {

				try {

					File processing = subf.get(i - 1);

					long file_size_k = processing.length() >> 10;

					doc_cn = new Document();

					doc_cn.add(new TextField("path", processing
							.getCanonicalPath(), Field.Store.YES));

					doc_cn.add(new LongField("modified", processing
							.lastModified(), Field.Store.YES));

					doc_cn.add(new LongField("size", file_size_k,
							Field.Store.YES));

					// TextField 更适合查询
					doc_cn.add(new TextField("name", processing.getName(),
							Field.Store.YES));

					Reader txtReader = new FileReader(processing);
					doc_cn.add(new TextField("contents", txtReader));

					if (!UpdateOrNot) {

						indexWriter_cn.addDocument(doc_cn);

					} else {
						indexWriter_cn.updateDocument(new Term("path",
								processing.getCanonicalPath()), doc_cn);
					}

					if (i % CommitSpace == 0) {

						indexWriter_cn.commit();
						doc_cn = null;

						Runtime runtime = Runtime.getRuntime();

						long UsedMemory = (runtime.totalMemory() - runtime
								.freeMemory()) >> 20;

						System.out
								.println("Thread " + this.getName()
										+ "  commit " + i + " UsedMemory:"
										+ UsedMemory);

						this.setPriority(Thread.MAX_PRIORITY);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}// for

			try {
				indexWriter_cn.forceMerge(120);
				// 强制合并策略:合并段直到有<= maxNumSegments
				// indexWriter.optimize();
				indexWriter_cn.commit();
				// this.setPriority(Thread.MIN_PRIORITY);
				doc_cn = null;

			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("thread " + this.getName() + " completed.");

			// try {
			// this.finalize();
			// } catch (Throwable e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			// }

		}// run

	}// WriteDocThread

	// --------------------------------------------------------------------------
	// Concurrent 并发 与多线程 性能区别不大 不建议使用
	// --------------------------------------------------------------------------
	public static void makeIndexAdvanceNext(String dataDir_s) throws Exception {

		File data_f = new File(dataDir_s);

		ArrayList<File> daf = findCompliantFile(data_f,
				"(.*\\.txt|.*\\.html|.*\\.HTM|.*\\.htm|.*\\.HTML)");

		// ---------分组处理------------------
		int fsize = daf.size();
		int gs = getGroupSize(fsize);
		int group = fsize / gs;

		System.out.println("All file :" + fsize + " Group:" + group
				+ " Group Size:" + gs + " Processors:"
				+ Runtime.getRuntime().availableProcessors() + "\n");

		// -----IndexWriter设置------------
		iwc.setMaxThreadStates(group + 2);
		iwc.setMaxBufferedDocs(fsize >> 2);

		System.out.println("\nRAMBufferSizeMB:" + iwc.getRAMBufferSizeMB()
				+ "\nDefaultWriteLockTimeout:"
				+ IndexWriterConfig.getDefaultWriteLockTimeout()
				+ "\nOpenMode:" + iwc.getOpenMode() + "\nIndexDeletionPolicy:"
				+ iwc.getIndexDeletionPolicy() + "\nWriteLockTimeout:"
				+ iwc.getWriteLockTimeout() + "\nMergePolicy:"
				+ iwc.getMergePolicy() + "\nMaxThreadStates:"
				+ iwc.getMaxThreadStates() + "\nReaderPooling:"
				+ iwc.getReaderPooling() + "\nRAMPerThreadHardLimitMB:"
				+ iwc.getRAMPerThreadHardLimitMB()
				+ "\nMaxBufferedDeleteTerms:" + iwc.getMaxBufferedDeleteTerms()
				+ "\nMaxBufferedDocs:" + iwc.getMaxBufferedDocs()
				+ "\nReaderTermsIndexDivisor:"
				+ iwc.getReaderTermsIndexDivisor() + "\nTermIndexInterval:"
				+ iwc.getReaderTermsIndexDivisor() + "\n");

		indexWriter_cn = new IndexWriter(nioD, iwc); // 重用已定义的IndexWriter
		// 一个IndexWriter对象可以被多个线程所共享

		List<WriteDocInConcurrent> wic_l = new ArrayList<WriteDocInConcurrent>();
		// http://stackoverflow.com/questions/3806919/using-add-for-arraylist-doesnt-seem-to-work-what-am-i-doing-wrong
		for (int i = 0; i < group; i++) {

			List<File> sub_l = daf.subList(i * gs, (i + 1) * gs);
			WriteDocInConcurrent wdi = new WriteDocInConcurrent(i, sub_l);
			wic_l.add(wdi);

		}

		// 分组结果可能会少计入【待处理文件】
		int Missing = fsize - (gs * group);

		if (Missing > 0) {
			wic_l.add(new WriteDocInConcurrent(-1, daf.subList(fsize - Missing,
					fsize)));
		}

		ExecutorService executor = Executors.newFixedThreadPool(3);
		// ExecutorService executor = Executors.newCachedThreadPool();
		executor.invokeAll(wic_l);
		// List<Future<Integer>> results = executor.invokeAll(wic_l);
		executor.shutdown();
		// www.oracle.com/technetwork/cn/articles/java/fork-join-422606-zhs.html
	}

	public static class WriteDocInConcurrent implements Callable<Integer> {

		List<File> subf;
		int n; // name
		int subf_l;
		int CommitSpace; // 提交间隔

		public WriteDocInConcurrent(int n, List<File> f) {
			this.n = n;
			this.subf = f;
			subf_l = subf.size();
			CommitSpace = getGroupSize(subf_l) * 7; // 提交间隔
		}

		public Integer call() throws Exception {

			// ---------------建立索引--------------------
			for (int i = 0; i < subf_l; i++) {

				try {

					Document doc_cn = new Document();

					File processing = subf.get(i);

					long file_size_k = processing.length() >> 10;

					doc_cn.add(new StringField("path", processing
							.getCanonicalPath(), Field.Store.YES));

					doc_cn.add(new LongField("modified", processing
							.lastModified(), Field.Store.YES));

					doc_cn.add(new LongField("size", file_size_k,
							Field.Store.YES));

					// TextField 更适合查询
					doc_cn.add(new TextField("name", processing.getName(),
							Field.Store.YES));

					Reader txtReader = new FileReader(processing);
					doc_cn.add(new TextField("contents", txtReader));

					indexWriter_cn.addDocument(doc_cn);

					if ((i != 0) & i % CommitSpace == 0) {
						indexWriter_cn.commit();
						System.out.println("Thread " + n + "commit " + i);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}// for

			try {
				indexWriter_cn.forceMerge(120);
				// 强制合并策略:合并段直到有<= maxNumSegments
				// indexWriter.optimize();
				indexWriter_cn.commit();
				// this.setPriority(Thread.MIN_PRIORITY);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("thread " + n + " completed.");

			return 0;

		} // call

	} // WriteDocInConcurrent
		// --------------------------------------------------------------------------

	// 查找符合要求的文件
	static ArrayList<File> findCompliantFile(File f, String compliant) {

		if (f.isFile()) {
			Compliance_f.add(f);
			return Compliance_f;
		}

		try { // listfiles 可能出错

			File[] ls_f = f.listFiles();

			// System.out.println("total files:" + ls_f.toString());

			int ll = ls_f.length;

			for (int i = 0; i < ll; i++) {
				if (ls_f[i].isFile() && ls_f[i].getName().matches(compliant)) {// 如果是文件,是否符合...表达式
					Compliance_f.add(ls_f[i]);// 添加到公共数组
				} else if (ls_f[i].isDirectory()) {// 如果是目录
					findCompliantFile(ls_f[i], compliant);// 递归调用
				}
			}

		} catch (Exception e) {

			// stackoverflow.com/questions/4812570/how-to-store-printstacktrace-into-a-string
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));

			System.err.println("\n\nfindCompliantFile 参数问题 察看文件参数是否正确\n"
					+ errors.toString() + "\n");

			System.out.println("Bye ~~~");
			System.exit(1); // 有错误退出，echo $?

		}

		return Compliance_f;
	}// findComplianceFile

	// 分组方法
	static int getGroupSize(int size) { // 确定组数
		int egn = 0;
		egn = size / ((int) (Math.log(size) / Math.log(2)) + 1);
		// System.out.print("\nTotal:" + size + " EveryGroup:" + egn);
		return egn;
	}

}// class end