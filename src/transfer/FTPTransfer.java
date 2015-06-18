package transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

public class FTPTransfer {

	private static final Logger logger = Logger.getLogger(FTPTransfer.class);

	private FTPClient ftp;

	private String host;
	private int port;
	private String userName;
	private String password;

	private String transferPath;

	private List<String> ignoreFolders;
	private List<String> ignoreFiles;

	private static String encode;

	private int count;

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

	}

	/**
	 * 
	 * @param path
	 *            上传到ftp服务器哪个路径下
	 * @param addr
	 *            地址
	 * @param port
	 *            端口号
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @return
	 * @throws Exception
	 */
	private boolean connect(String path, String addr, int port, String username, String password) throws Exception {
		boolean result = false;
		ftp = new FTPClient();
		int reply;
		ftp.connect(addr, port);
		ftp.login(username, password);
		ftp.setDataTimeout(1000 * 60 * 60);
		ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		reply = ftp.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			return result;
		}
		ftp.changeWorkingDirectory(path);
		result = true;
		return result;
	}

	/**
	 * 
	 * @param file
	 *            上传的文件或文件夹
	 * @throws Exception
	 */
	private void upload(File file) throws Exception {
		if (file.isDirectory()) {
			ftp.makeDirectory(file.getName());
			ftp.changeWorkingDirectory(file.getName());

			logger.debug("current dir:" + ftp.printWorkingDirectory());

			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				File childFile = files[i];
				if (childFile.isDirectory()) {
					if (ignore(childFile)) {
						logger.debug("ignore dir: " + childFile.getPath());
						continue;
					} else {
						upload(childFile);
						ftp.changeToParentDirectory();
					}
				} else {
					upload(childFile);
				}
			}
		} else {
			if (ignore(file)) {
				logger.debug("ignore file: " + file.getPath());
				return;
			}
			if (needTransferByModTime(file)) {
				FileInputStream input = new FileInputStream(file);
				logger.info("upload file:" + ftp.printWorkingDirectory() + "/" + file.getName());
				ftp.storeFile(encodedfileNameToFtp(file.getName()), input);
				input.close();
				count++;
			}
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

	private boolean needTransferByModTime(File file) throws IOException {

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

	@SuppressWarnings("unused")
	private void printTransferInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("transfer info:\n");
		sb.append(" host:" + this.host + "\n");
		sb.append(" transfer path: " + this.transferPath);

		System.out.println(sb);
	}

	public static void transferByProperty(Properties property) throws IOException {
		FTPTransfer t = new FTPTransfer(property);

		try {
			logger.info("transfer from [" + t.transferPath + "] to [" + t.host + "]");
			t.connect("", t.host, t.port, t.userName, t.password);
			File file = new File(t.transferPath);
			t.upload(file);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			t.ftp.disconnect();

			logger.info("传输总数: " + t.count);
		}

	}

	public static void main(String[] args) throws Exception {
		Properties property = getFTPProperties("transfer/ftp.properties");
		FTPTransfer t = new FTPTransfer(property);
		t.connect("", t.host, t.port, t.userName, t.password);
		File file = new File(t.transferPath);
		t.upload(file);

		t.ftp.disconnect();

		System.out.println("传输总数: " + t.count);
	}
}