package tk.wuwenie.luceneSearch;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IOContext.Context;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * 
 * @author wuwenjie
 * @import lucene-core-4.3.0.jar lucene-analyzers-common-4.3.0.jar
 *         lucene-queryparser-4.3.0.jar
 * 
 * @demo lucene-4.3.0/docs/demo/src-html/org/apache/lucene/demo/SearchFiles.html
 * 
 * 
 */
public class TxtFileSearcher {

	static SortField docNum = SortField.FIELD_DOC; // 按文档编号排序 index
	static SortField docDefault = SortField.FIELD_SCORE;

	// 按文档得分排序，相关性relevance

	// 普通查询
	public static void search(String indexDir_s, String searchstr)
			throws Exception {

		// This is the directory that hosts the Lucene index
		File indexDir = new File(indexDir_s);
		FSDirectory directory = FSDirectory.open(indexDir);
		// FSDirectory index_dir = FSDirectory.open(indexDir);//
		// (indexDir,null);

		IndexReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);
		if (!indexDir.exists()) {
			System.err.println("The Lucene index is not exist");
			return;
		}

		Term term = new Term("contents", searchstr);// queryStr.toLowerCase()
		TermQuery luceneQuery = new TermQuery(term);
		TopDocs results = searcher.search(luceneQuery, 10);
		// public TopDocs search(Query query, int n)throws IOException
		// Finds the top n hits for query.
		ScoreDoc[] hits = results.scoreDocs;
		// ScoreDoc[] Class TopDocs.scoreDocs ,The top hits for the query.
		int numTotalHits = results.totalHits;
		// The total number of hits for the query.
		System.out.println(numTotalHits + " total matching documents");

		for (int i = 0; i < hits.length; i++) {

			Document document = searcher.doc(hits[i].doc);
			// Class ScoreDoc.doc int A hit document's number.

			String path = document.get("path");
			// Class Document get public final String get(String name)
			// 返回name对应的字符串
			System.out.println("File No:" + i + 1 + "\n\tPath:" + path
					+ "\n\tinfo:" + hits[i].toString() + "\n");

			Explanation explanation = searcher
					.explain(luceneQuery, hits[i].doc);
			// 返回一个说明文档介绍了如何对查询发还

			System.out.println("\tDescribes the score : \n\t\t"
					+ explanation.toString() + "\n");
		}

		reader.close();
		directory.close();

	}// search

	// 支持语句查询
	public static void QueryParserSearch(String indexDir_s, String searchstr,
			String range) {

		long startTime = new Date().getTime();

		// ---------打开Index索引--------------
		File indexDir = new File(indexDir_s);
		Directory indexD = null;
		try {
			indexD = FSDirectory.open(indexDir);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		RAMDirectory directory = null;
		try {
			IOContext ioContext = new IOContext(Context.DEFAULT);
			directory = new RAMDirectory(indexD, ioContext);
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		// ----------索引阅读器-------------
		IndexReader reader = null;
		try {
			reader = DirectoryReader.open(directory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// ---------索引搜索器------------------
		IndexSearcher searcher = new IndexSearcher(reader);

		if (!indexDir.exists()) {
			System.out.println("The Lucene index is not exist");
			return;
		}

		// --------支持语句查询de查询类-----------
		Analyzer luceneAnalyzer = new StandardAnalyzer(Version.LUCENE_44);
		QueryParser qupa = new QueryParser(Version.LUCENE_44, range,
				luceneAnalyzer);
		// public QueryParser(Version matchVersion,String f, Analyzer a)
		// Create a query parser.
		// Parameters:matchVersion - Lucene version to match. See above.
		// f - the default field for query terms.
		// a - used to find terms in the query text.

		Query qParser = null;
		try {
			qParser = qupa.parse(searchstr);// 解析语句
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// ---------查找-------------
		TopDocs results2 = null;
		try {
			results2 = searcher.search(qParser, 5);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ScoreDoc[] hits2 = results2.scoreDocs;

		int numTotalHits2 = results2.totalHits;
		System.out.println(numTotalHits2 + " total matching documents");

		for (int i = 0; i < hits2.length; i++) {
			Document document = null;
			try {
				document = searcher.doc(hits2[i].doc);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String path2 = document.get("path");
			System.out.println("qParser File: " + hits2[i].toString() + ":"
					+ path2);
		}

		// ------------关闭--------------
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		directory.close();

		try {
			indexD.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ------------计时-------------
		long endTime = new Date().getTime();
		System.out.println("\nIt takes " + (endTime - startTime)
				+ " milliseconds to QueryParserSearch");

	}// QueryParserSearch

	// 支持结果排序的查询,欲提高搜索效率可，缓存之前的结果，太老的可挤出【缓存队列】
	// 持续化查询用RAMDirectory比较好，允许对同一个索引运行多个并行的搜索进程
	public static void SortQueryParserSearch(String indexDir_s,
			String searchstr, String SearchRange, String order)
			throws Exception {

		long startTime = new Date().getTime();

		// ----------RAMDirectory-----------
		File indexDir = new File(indexDir_s);
		if (!indexDir.exists()) {
			System.err.println("The Lucene index is not exist! Exit!");
			return;
		}
		FSDirectory FSdirectory = FSDirectory.open(indexDir);
		// IOContext ioContext = new IOContext(Context.DEFAULT);
		// RAMDirectory RAMdry = new RAMDirectory(FSdirectory, ioContext);

		IndexReader reader = DirectoryReader.open(FSdirectory); // RAMdry
		IndexSearcher searcher = new IndexSearcher(reader);

		// --------支持语句查询de查询类-----------
		Analyzer cluceneAnalyzer = new CJKAnalyzer(Version.LUCENE_44);// 中文分析器
		QueryParser qupa = new QueryParser(Version.LUCENE_44, SearchRange,
				cluceneAnalyzer);
		Query qParser = qupa.parse(searchstr);

		// ----------查询结果排序----------------
		// 可以多个排序方法 blog.csdn.net/jadyer/article/details/10064105
		// sort - The Sort object 域排序信息的有序封装集合
		Sort sort = new Sort();

		if (order == "index") {
			docDefault = docNum;
		} else if (order == "modified") {
			docDefault = new SortField(order, SortField.Type.LONG);
		} else if (order == "size") {
			docDefault = new SortField(order, SortField.Type.LONG);
		} else if (order != null) {
			docDefault = new SortField(order, SortField.Type.STRING);
		}
		sort.setSort(docDefault);

		// TopDocs results = searcher.search(qParser, 10, sort);
		// public TopFieldDocs search(Query query,int n,Sort sort)

		int numHits = 70;
		// 排序设置器
		// sujitpal.blogspot.jp/2010/02/handling-lucene-hits-deprecation-in.html
		TopFieldCollector collector = TopFieldCollector.create(sort, numHits,
				true, true, true, sort == null);

		/*
		 * create
		 * 
		 * public static TopFieldCollector create(Sort sort, int numHits,
		 * boolean fillFields, boolean trackDocScores, boolean trackMaxScore,
		 * boolean docsScoredInOrder) throws IOException
		 * 
		 * Creates a new TopFieldCollector from the given arguments.
		 * 
		 * NOTE: The instances returned by this method pre-allocate a full array
		 * of length numHits.
		 * 
		 * Parameters: sort - 结果排序 the sort criteria (SortFields).
		 * 
		 * numHits - 结果数量 the number of results to collect.
		 * 
		 * fillFields - 是否返回实际的域值 specifies whether the actual field values
		 * should be returned on the results (FieldDoc).
		 * 
		 * trackDocScores - 是否跟踪文档分数 specifies whether document scores should be
		 * tracked and set on the results. Note that if set to false, then the
		 * results' scores will be set to Float.NaN. Setting this to true
		 * affects performance, as it incurs the score computation on each
		 * competitive result. Therefore if document scores are not required by
		 * the application, it is recommended to set it to false.
		 * 
		 * trackMaxScore - 是否跟踪最大分数 specifies whether the query's maxScore
		 * should be tracked and set on the resulting TopDocs. Note that if set
		 * to false, TopDocs.getMaxScore() returns Float.NaN. Setting this to
		 * true affects performance as it incurs the score computation on each
		 * result. Also, setting this true automatically sets trackDocScores to
		 * true as well.
		 * 
		 * docsScoredInOrder 文档是否按文档编号排- specifies whether documents are scored
		 * in doc Id order or not by the given Scorer in
		 * Collector.setScorer(Scorer).
		 * 
		 * Returns: a TopFieldCollector instance which will sort the results by
		 * the sort criteria.
		 * 
		 * Throws: IOException - if there is a low-level I/O error
		 */

		searcher.search(qParser, collector);
		// public void search(Query query, Collector results) throws IOException
		// Lower-level search API.
		// Collector.collect(int) is called for every matching document.
		TopDocs results = collector.topDocs();

		// MultiFieldQueryParser
		// http://blog.csdn.net/lizhihai_99/article/details/5559423

		// ----------------------
		// setSimilarity getSimilarity
		// termStatistics
		// ----------------------

		ScoreDoc[] hits = results.scoreDocs;
		// ScoreDoc[] Class TopDocs.scoreDocs ,The top hits for the query.
		int numTotalHits = results.totalHits;
		// The total number of hits for the query.
		System.out.println(numTotalHits + " total matching documents");

		for (int i = 0; i < hits.length; i++) {

			Document document = searcher.doc(hits[i].doc);
			// Class ScoreDoc.doc int A hit document's number.

			String path = document.get("path");
			String name = document.get("name");
			Long modified = Long.parseLong(document.get("modified"));

			Date mod = new Date(modified);

			Long size = Long.parseLong(document.get("size"));

			// Class Document get public final String get(String name)
			// 返回name对应的字符串
			System.out.println("File No:" + i + "\tdoc:" + hits[i].doc
					+ " score:" + hits[i].score + "\n\tPath:" + path
					+ "\n\tName:" + name + "\tModified:" + mod.toString()
					+ "\n\tSize:" + size + "\n");

			// Explanation explanation = searcher.explain(qParser, hits[i].doc);
			// 返回一个说明文档介绍了如何对查询发还
			// System.out.println("\tDescribes the score : \n\t\t"
			// + explanation.toString() + "\n");
		}

		reader.close();
		// RAMdry.close();
		FSdirectory.close();

		// ------------计时-------------
		long endTime = new Date().getTime();
		System.out.println("\nIt takes " + (endTime - startTime)
				+ " milliseconds in SortQueryParserSearch.");

	}// SortQueryParserSearch

}