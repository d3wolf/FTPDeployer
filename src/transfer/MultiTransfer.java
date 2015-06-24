package transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;

public class MultiTransfer implements Runnable {

	private File file;
	private FTPClient ftp;
	
	private TransCounter counter;
	
	public void setCounter(TransCounter counter) {
		this.counter = counter;
	}

	private String workingDir;
	private String host;
	private int port;
	private String userName;
	private String password;

	public MultiTransfer(String workingDir, String host, int port, String userName, String password, File file) {
		this.file = file;
		this.workingDir = workingDir;
		this.host = host;
		this.port = port;
		this.userName = userName;
		this.password = password;
	}

	@Override
	public void run() {
		try {
			ftp = FTPTransfer.getFtp(workingDir, host, port, userName, password);

			if (file.isDirectory()) {
				ftp.makeDirectory(file.getName());
				ftp.changeWorkingDirectory(file.getName());
			} else {
				if (FTPTransfer.needTransferByModTime(ftp, file)) {
					FileInputStream input = new FileInputStream(file);
					ftp.storeFile(FTPTransfer.encodedfileNameToFtp(file.getName()), input);
					input.close();
					FTPTransfer.logger.info(Thread.currentThread().getName() + "->upload success:" + ftp.printWorkingDirectory() + "/" + file.getName());
					counter.countUp();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (ftp != null) {
				try {
					ftp.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
