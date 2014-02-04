package tk.wuwenie.luceneSearch;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

//time java -Xms512m -Xmx1024m -jar lucene.jar -AI /media/linux_lenovo/file /media/linux_lenovo/L_index

//time java -Xms512m -Xmx1024m -jar lucene.jar -SQPS /media/linux_lenovo/L_index 张飞 contents contents
//time java -Xms512m -Xmx1024m -jar lucene.jar -SQPS /media/linux_lenovo/L_index 春秋 contents ""

public class Main_lucene {

	static boolean execMakeIndex = false;
	static TxtFileIndexer fi;
	static boolean UseRAM = false;
	static boolean showToken = false;
	static String file_tobeIndex;
	static String file_saveIndex;

	// main 命令行分析
	public static void main(String[] args) {

		// System.out.println("args length:" + args.length);

		// for (int i = 0; i < args.length; i++) {
		// System.out.println("args[" + i + "]" + ":" + args[i]);
		// }

		if (args.length != 0) {

			for (int i = 0; i < args.length; i++) {
				// System.out.println("args[" + i + "]" + ":" + args[i]);

				if (args[i].equals("-I")) { // 如果是生成索引

					if (args.length >= i + 3) {
						execMakeIndex = true;
						file_tobeIndex = args[i + 1];
						file_saveIndex = args[i + 2];
					} else {
						System.err.println("-I file_tobeIndex file_saveIndex ");
					}
				} else if (args[i].equals("-UseRAM")) {
					UseRAM = true;

				} else if (args[i].equals("-showToken")) {
					showToken = true;

				} else if (args[i].equals("-AI")) { // 如果是增强建立索引

					if (args.length >= i + 3) {
						makeIndexAdvance(args[i + 1], args[i + 2]);
					}

				} else if (args[i].equals("-AIN")) { // 如果是增强建立索引

					if (args.length >= i + 3) {
						makeIndexAdvanceNext(args[i + 1], args[i + 2]);
					}
				} else if (args[i].equals("-AI2")) { // 如果是增强建立索引

					if (args.length >= i + 3) {
						makeIndexAdvance2(args[i + 1], args[i + 2]);
					}

				} else if (args[i].equals("-ADD")) { // 如果是合并两个索引

					if (args.length >= i + 3) {
						addIndexToIndex(args[i + 1], args[i + 2]);
					} else {
						System.err
								.println("-ADD [dir_index-src] [dir_index-be-add]");
					}
				} else if (args[i].equals("-S")) {// 如果是普通查询

					if (args.length >= i + 3) {

						Serach(args[i + 1], args[i + 2]);

					} else {
						System.err.println("-S file_saveIndex Query");
					}

				} else if (args[i].equals("-QPS")) { // 如果是语句查询

					if (args.length >= i + 4) {

						QPSearch(args[i + 1], args[i + 2], args[i + 3]);

					} else {
						System.err.println("-QPS file_saveIndex Query Range");
					}

				} else if (args[i].equals("-SQPS")) { // 如果是结果排序查询

					if (args.length >= i + 5) {

						try {
							SQPSearch(args[i + 1], args[i + 2], args[i + 3],
									args[i + 4]);
						} catch (Exception e) {
							e.printStackTrace();
						}

					} else {
						System.err
								.println("-SQPS file_saveIndex Query Range Order");
					}
				}

				else if (args[i].equals("-help")) {
					printHelp();
				}
			}// for

			if (execMakeIndex) {
				makeIndex(file_tobeIndex, file_saveIndex, UseRAM, showToken);
			}
			// System.out.println("unknown argument '" + args[0] + "'");

		}// if length !=0 end
		else {
			System.out.println("See more -help");
		}

	}// main

	private static void addIndexToIndex(String string, String string2) {
		try {
			TxtFileIndexer.addIndexToIndex(string, string2);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void makeIndexAdvance2(String f, String i) {

		try {
			TxtFileIndexer.IndexerInAdvance(i);
			TxtFileIndexer.makeIndexAdvance2(f);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void makeIndexAdvanceNext(String f, String i) {
		try {
			TxtFileIndexer.IndexerInAdvance(i);
			TxtFileIndexer.makeIndexAdvanceNext(f);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void makeIndexAdvance(String f, String i) {

		try {
			TxtFileIndexer.IndexerInAdvance(i);
			TxtFileIndexer.makeIndexAdvance(f);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void SQPSearch(String indexDir_s, String searchstr,
			String SearchRange, String order) throws Exception {
		// System.out
		// .println("\n\n---------TxtFileSearcher.SortQueryParserSearch---------\n\n");
		TxtFileSearcher.SortQueryParserSearch(indexDir_s, searchstr,
				SearchRange, order);
		System.exit(0);

	}

	public static void QPSearch(String indexDir_s, String searchstr,
			String range) {

		// System.out
		// .println("\n\n---------TxtFileSearcher.QueryParserSearch---------\n\n");
		TxtFileSearcher.QueryParserSearch(indexDir_s, searchstr, range);

		// System.out.println("\n\n");

	}

	public static void makeIndex(String file_tobeIndex, String file_saveIndex,
			boolean U, boolean T) {

		try {
			fi = new TxtFileIndexer(file_tobeIndex, file_saveIndex);
			fi.makeIndex(U, T);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	static void Serach(String file_saveIndex, String Query) {
		try {

			TxtFileSearcher.search(file_saveIndex, Query);

			// System.out.println("\n\n");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}// Serach

	// 显示帮助
	private static void printHelp() {

		String ver = "LUCENE_46";

		String help_s = "\nLuceneSearch\n"
				+ "v0.0.1 20130526-20130713\n"
				+ "v0.0.2 20130713-20130728\n"
				+ "v0.0.3 20130728-20130817\n"
				+ "v0.0.4 20130817-20130831\n"
				+ "v0.0.5 20130831-20130903\n"
				+ "v0.0.6 20130903-20130906\n"
				+ "v0.0.7 20130906-20130920\n"
				+ "v0.0.8 20130920-20131103\n"
				+ "v0.0.9 20131103-20131124\n"
				+ "v0.0.10 20131124-20131201\n"
				+ "v0.0.11 20131201-20131208\n"
				+ "v0.0.12 20131208-20140112\n"
				+ "v0.0.13 20140112-20140118\n"
				+ "\nAuthor wuwenjie\n"
				+ "PowerBy Lucene "
				+ ver
				+ "\nThankFor Eclipse;Java;Xubuntu;GUN/Linux\n"
				+ "Bug Roprt:http://www.wuwenjie.tk OR http://www.wuhuixin.tk\n\n"
				+ "UseAge:\n"
				+ "-I make Index \t-I [file_tobeIndex] [file_saveIndex] {-UseRAM} -{showToken}\n"
				+ "-AI make Index In Advance \t -AI [file_tobeIndex] [file_saveIndex]\n"
				+ "-AI2 make Index In Advance \t -AI2 [file_tobeIndex] [file_saveIndex]\n"
				+ "-ADD add index to a index \t -ADD [dir_index-src] [dir_index-be-add]\n"
				+ "-S search \t-S [file_saveIndex] [Query]\n"
				+ "-QPS QueryParserSearch \t-QPS [Index] [SpecialQuery] [Range]\n"
				+ "-SQPS SortQueryParserSearch \t-SQPS [file_Index] [Query] [SearchRange] [Order]\n"
				+ "\t\t[SearchRange] contents name size modified path\n"
				+ "\t\t[Order] Null {Default,score,relevance} index path modified size contents\n"
				+ "-help help\n";

		System.out.println(help_s);

		showGUI("帮助-help", help_s);

	}

	// GUI
	public static void showGUI(String t, String context) {

		final Frame f = new Frame();
		f.setTitle(t);
		f.setResizable(true);// 设置可调整大小
		f.setSize(500, 275);
		f.setLocation(250, 500);

		Font font = new Font(null, Font.PLAIN, 20);

		TextArea ta = new TextArea(context);
		ta.setSize(220, 225);
		ta.setFont(font);
		ta.setBackground(Color.BLACK);
		ta.setForeground(Color.GREEN);

		f.add(ta);
		f.setVisible(true);

		f.addWindowListener(new WindowAdapter() {// 关闭窗口
			@Override
			public void windowClosing(WindowEvent e) {
				f.dispose(); // 释放f及其子类的所有资源
				System.exit(0);
			}
		});

	}// showGUI

}
