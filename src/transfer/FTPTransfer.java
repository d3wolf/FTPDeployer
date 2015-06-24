package transfer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

public class FTPTransfer {

	public static final Logger logger = Logger.getLogger(FTPTransfer.class);

	private String host;
	private int port;
	private String userName;
	private String password;

	private String transferPath;

	private List<String> ignoreFolders;
	private List<String> ignoreFiles;

	private static String encode;

	private int count;

	private TransCounter counter;
	
	private ExecutorService pool;

	@SuppressWarnings("unused")
	private FTPTransfer() {

	}

	public FTPTransfer(Properties property) {
		this.count = 0;
		this.host = property.getProperty("FTP_HOST");
		this.port = Integer.parseInt(property.getProperty("FTP_PORT"));
		this.userName = property.getProperty("FTP_USERNAME");
		this.password = property.getProperty("FTP_PASSWORD");
		this.transferPath = property.getProperty("TRANSFER_PATH");
		this.ignoreFolders = Arrays.asList(property.getProperty("IGNORE_FOLDER").split(","));
		this.ignoreFiles = Arrays.asList(property.getProperty("IGNORE_FILE").split(","));
		encode = property.getProperty("LOCAL_FILE_NAME_ENCODE");
		count = 0;
		counter = new TransCounter();
		
		pool = Executors.newFixedThreadPool(5);

	}

	public static FTPClient getFtp(String path, String addr, int port, String username, String password) throws SocketException, IOException {
		FTPClient ftp = new FTPClient();
		ftp.connect(addr, port);
		ftp.login(username, password);
		ftp.setDataTimeout(1000 * 60 * 60);
		ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
	
		ftp.changeWorkingDirectory(path);
		return ftp;
	}

	/**
	 * 创建多个FTPClient对象来上传
	 * 
	 * @param ftp
	 * @param file
	 * @throws Exception
	 */
	private void upload(String workingDir, String host, int port, String userName, String password, File file) throws Exception {
		if (file.isDirectory()) {

			logger.debug("current dir:" + workingDir);
			
			FTPClient ftp = getFtp(workingDir, host, port, userName, password);
			ftp.makeDirectory(file.getName());
			ftp.changeWorkingDirectory(file.getName());
			
			ftp.disconnect();
			
			workingDir = workingDir + File.separator + file.getName();

			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				File childFile = files[i];
				if (ignore(childFile)) {
					logger.info("ignore dir: " + childFile.getPath());
					continue;
				} else {
					upload(workingDir, host, port, userName, password, childFile);
				}
			}
		} else {
			if (ignore(file)) {
				logger.debug("ignore file: " + file.getPath());
				return;
			}

			MultiTransfer run = new MultiTransfer(workingDir, host, port, userName, password, file);
			
			run.setCounter(counter);
			
			Thread thread = new Thread(run);
			pool.execute(thread);

			// MultiTransferWithReturn run = new MultiTransferWithReturn(workingDir, host, port, userName, password, file);
			// if((Boolean)pool.submit(run).get()){ count++; }

			count++;
		
		}

	}

	/**
	 * 中文名文件传输异常处理
	 * 
	 * @param obj
	 * @return
	 */
	public static String encodedfileNameToFtp(String obj) {
		try {
			if (obj == null) {
				return null;
			} else {
				return new String(obj.toString().getBytes(encode), "iso-8859-1");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return obj;
		}
	}

	public static String encodedfileNameFromFtp(String obj) {
		try {
			if (obj == null) {
				return null;
			} else {
				return new String(obj.toString().getBytes("iso-8859-1"), encode);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return obj;
		}
	}

	public static boolean needTransferByModTime(FTPClient ftp, File file) throws IOException {

		boolean needTransfer = true;
		FTPFile[] ftpFiles = ftp.listFiles();
		for (FTPFile ftpFile : ftpFiles) {
			if (encodedfileNameFromFtp(ftpFile.getName()).equals(file.getName())) {
				Long time = file.lastModified();
				Calendar cd = Calendar.getInstance();
				cd.setTimeInMillis(time);
				if (ftpFile.getTimestamp().after(cd)) {
					needTransfer = false;
					break;
				}
			}
		}

		return needTransfer;
	}

	private boolean ignore(File file) {
		boolean ignore = false;
		if (file.isDirectory()) {
			if (ignoreFolders.contains(file.getName())) {
				ignore = true;
			}
		} else if (file.isFile()) {
			if (ignoreFiles.contains(file.getName())) {
				ignore = true;
			}
		}

		return ignore;
	}

	public static Properties getFTPProperties(String resourcePath) {
		Properties properties = new Properties();
		InputStream inputstream = null;
		try {
			inputstream = ((FTPTransfer.class).getClassLoader()).getResourceAsStream(resourcePath);
			try {
				if (inputstream != null)
					properties.load(inputstream);
			} catch (IOException ex) {
			}
		} catch (Throwable throwable) {
			throwable.printStackTrace(System.err);
			throw new ExceptionInInitializerError(throwable);
		} finally {
			if (inputstream != null) {
				try {
					inputstream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return properties;
	}

	public static void transferByProperty(Properties property) throws IOException {
		FTPTransfer t = new FTPTransfer(property);

		try {
			logger.info("transfer from [" + t.transferPath + "] to [" + t.host + "]");
			File file = new File(t.transferPath);
			t.upload("", t.host, t.port, t.userName, t.password, file);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			t.pool.shutdown();

		//	logger.info("传输总数: " + t.counter.getCount());
		}

	}

	public static void main(String[] args) throws Exception {

	}
}