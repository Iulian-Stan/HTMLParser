package htmlParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.swing.text.html.HTMLEditorKit;

import dnsResolver.DNSResolver;
import dnsResolver.DNSResponse;
import httpCrawler.HTTPCrawler;
import urlParser.URL;

public class Main {

	public static void main(String[] args) {

		Properties props = new Properties();

		try {
			File configFile = new File("config.properties");
			InputStream inputStream = new FileInputStream(configFile);
			props.load(inputStream);
		} catch (FileNotFoundException e) {
			System.err.println("Config file not found");
		} catch (IOException e) {
			System.err.println("Some IO exception on config load");
		}

		String link = props.getProperty("url", "http://www.w3.org/");
		String logFile = props.getProperty("logFile", "log.txt");
		
		HTTPCrawler crawler = null;
		URL url = null;

		if (null == (url = URL.ParseURL(link)))
			return;
		
		try {
			if (null == (crawler = InitCrawler(url))) {
				return;
			}
			
			crawler.GetHeader();
			crawler.WriteLogHtml(logFile);
			Parse(url, new FileReader(logFile));
			crawler.Close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	// initializes the parser and calls its main method
	private static void Parse(URL url, Reader reader) throws IOException {
		HTMLParserGetter kit = new HTMLParserGetter();
		HTMLEditorKit.Parser parser = kit.getParser();
		HTMLParserCallBacks callback = new HTMLParserCallBacks(url);
		parser.parse(reader, callback, true);
	}
	
	private static HTTPCrawler InitCrawler(URL url) {
		String ip = null;

		if (null == (ip = GetIpAddress(url.getHost()))) {
			System.err.println("Could not resolve hostname to ip address");
			return null;
		}

		try {
			HTTPCrawler crawler = new HTTPCrawler(ip, url.getPort());
			String response = crawler.HTTPQuery("GET", url.getHost(), url.getResurce());

			if (response.contains("20"))
				return crawler;
			else if (response.contains("30")) {
				String redirect = null;
				if (null == (redirect = crawler.GetRedirectLocation())) {
					System.err.println("Could not find redirect location");
					return null;
				}
				return InitCrawler(redirect);
			} else
				return null;
		} catch (IOException e) {
			System.err.print("IOException in crawler");
			return null;
		}
	}
	
	private static HTTPCrawler InitCrawler(String link) {
		URL url = null;

		if (null == (url = URL.ParseURL(link)))
			return null;

		return InitCrawler(url);
	}
	
	private static String GetIpAddress(String host) {
		String ipAddress = null;
		DNSResponse dnsResp;
		if (DNSResolver.IsValidHosName(host)) {
			dnsResp = DNSResolver.GetIpByHostName(host);
			if (null != dnsResp) {
				ipAddress = dnsResp.RDATA();
			}
		}
		return ipAddress;
	}
}