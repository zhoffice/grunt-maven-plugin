package cn.com.cnwidsom;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal build
 * @phase prepare-package
 * @requiresProject false
 */
public class Main extends AbstractMojo {

	private static final String GRUNT_FILE_NAME = "Gruntfile.js";
	private static final String GRUNT_FILE_CONFIG = "package.json";

	private static final int OS_TYPE_WIN = 1;
	private static final int OS_TYPE_UNIX_LIKE = 2;

	private static final String[] GRUNT_INSTALL_WIN_COMMAND = new String[] { "cmd.exe", "/c", "npm", "install", "grunt", "--save-dev" };
	private static final String[] GRUNT_UPDATE_WIN_COMMAND = new String[] { "cmd.exe", "/c", "npm", "install" };
	private static final String[] GRUNT_BUILD_WIN_COMMAND = new String[] { "cmd.exe", "/c", "grunt" };

	private static final String[] GRUNT_INSTALL_UNIX_COMMAND = new String[] { "npm", "install", "grunt", "--save-dev" };
	private static final String[] GRUNT_UPDATE_UNIX_COMMAND = new String[] { "npm", "install" };
	private static final String[] GRUNT_BUILD_UNIX_COMMAND = new String[] { "grunt" };
	/**
	 * @parameter property="gruntPath"
	 */
	String gruntPath;
	/**
	 * @parameter property="outputDir"
	 */
	String outputDir;
	/**
	 * @parameter property="copyTo"
	 */
	String copyTo;

	/**
	 * @parameter property="autoUpdate"
	 */
	boolean autoUpdate = true;

	private int getOSType() {
		Properties prop = System.getProperties();

		String os = prop.getProperty("os.name");
		System.out.println(os);

		if (os.toLowerCase().startsWith("win")) {
			return OS_TYPE_WIN;
		} else {
			return OS_TYPE_UNIX_LIKE;
		}
	}

	// npm install grunt --save-dev
	// npm install
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			String[] GRUNT_INSTALL_COMMAND = null;
			String[] GRUNT_UPDATE_COMMAND = null;
			String[] GRUNT_BUILD_COMMAND = null;
			if (getOSType() == OS_TYPE_WIN) {
				GRUNT_INSTALL_COMMAND = GRUNT_INSTALL_WIN_COMMAND;
				GRUNT_UPDATE_COMMAND = GRUNT_UPDATE_WIN_COMMAND;
				GRUNT_BUILD_COMMAND = GRUNT_BUILD_WIN_COMMAND;
			} else if (getOSType() == OS_TYPE_UNIX_LIKE) {
				GRUNT_INSTALL_COMMAND = GRUNT_INSTALL_UNIX_COMMAND;
				GRUNT_UPDATE_COMMAND = GRUNT_UPDATE_UNIX_COMMAND;
				GRUNT_BUILD_COMMAND = GRUNT_BUILD_UNIX_COMMAND;
			}
			if (StringUtils.isNotEmpty(gruntPath)) {
				File f = new File(gruntPath);
				if (!f.exists()) {
					throw new MojoFailureException("GruntPath " + f.getAbsolutePath() + " is not exists.");
				} else {
					File[] files = f.listFiles(new FilenameFilter() {
						public boolean accept(File file, String name) {
							if (name.equalsIgnoreCase(GRUNT_FILE_NAME) || name.equalsIgnoreCase(GRUNT_FILE_CONFIG))
								return true;
							return false;
						}
					});
					if (files == null || files.length < 2) {
						throw new MojoFailureException("Not found " + GRUNT_FILE_NAME + " and " + GRUNT_FILE_CONFIG + " in " + f.getAbsolutePath() + ".");
					}
				}
				Process p = null;
				if (autoUpdate) {
					getLog().info("Updating grunt...");
					p = Runtime.getRuntime().exec(GRUNT_INSTALL_COMMAND, null, f);
					waitForStdOut(p);
					p.destroy();

					getLog().info("Updating grunt plugins and build...");
					p = Runtime.getRuntime().exec(GRUNT_UPDATE_COMMAND, null, f);
					waitForStdOut(p);
					p.destroy();
				}
				getLog().info("Running grunt....");
				p = Runtime.getRuntime().exec(GRUNT_BUILD_COMMAND, null, f);
				waitForStdOut(p);
				p.destroy();

				if (StringUtils.isNotEmpty(outputDir) && StringUtils.isNotEmpty(copyTo)) {
					File src = new File(outputDir);
					File dest = new File(copyTo);
					if (src.exists()) {
						if (!dest.exists()) {
							if (!dest.mkdirs()) {
								throw new MojoFailureException(copyTo + " is not exist.");
							}
						}
						if (getOSType() == OS_TYPE_WIN)
							copyDirectoryOnWindows(src, dest);
						else if (getOSType() == OS_TYPE_UNIX_LIKE) {
							copyDirectoryOnUnix(src, dest);
						}
					} else {
						throw new MojoFailureException(outputDir + " is not exist.");
					}
				} else {
					getLog().info("destDir or copyTo is null, skip copy resource.");
				}
				getLog().info("Process finished.");
			} else {
				getLog().info("Cannot find gruntPath, skip grunt process.");
			}
		} catch (Exception ex) {
			throw new MojoFailureException("", ex);
		}
		getLog().info(this.toString());
	}

	private void waitForStdOut(Process p) throws IOException {
		InputStream is = p.getInputStream();
		int len = 0;
		byte[] b = new byte[4096];
		while (len >= 0) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((len = is.read(b)) > 0) {
				baos.write(b, 0, len);
			}
			getLog().info(new String(baos.toByteArray()));
		}
	}

	private void copyDirectoryOnWindows(File srcFile, File destFile) throws IOException {
		getLog().info("copying resources from " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
		Process p = Runtime.getRuntime().exec(
				new String[] { "cmd.exe", "/c", "xcopy", srcFile.getAbsolutePath(), destFile.getAbsolutePath(), "/S", "/E", "/Y" });
		waitForStdOut(p);
		p.destroy();
	}

	private void copyDirectoryOnUnix(File srcFile, File destFile) throws IOException {
		getLog().info("copying resources from " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
		Process p = Runtime.getRuntime().exec(new String[] { "cp -rf ", srcFile.getAbsolutePath(), destFile.getAbsolutePath(), "/S", "/E", "/Y" });
		waitForStdOut(p);
		p.destroy();
	}
}
