package infosistema.openbaas.utils;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Log {

	private static Logger logger = null;

	private static final int TRACE = 0; 
	private static final int DEBUG = 1;
	private static final int INFO = 2;
	private static final int WARNING = 3;
	private static final int ERROR = 4;
	private static final int FATAL = 5;
	
	static {
		if (logger == null) initLogger();
	}

	public static synchronized void initLogger() {
		logger = Logger.getLogger("openBaas@infosistema");
		Properties props = LoadProperties.getProperties("infosistema.openbaas.utils.properties.log4j");
		PropertyConfigurator.configure(props);
	}

	private static void log(int logLevel, String asUser, String asCallerObject, String asMethodName, String asMessage, Throwable t) {
		String sClass = asCallerObject;
		String sMethod = (asMethodName == null || "".equals(asMethodName)) ? "none" : asMethodName;
		String sUser = (asUser == null || "".equals(asUser)) ? "anonymous" : asUser + " in ";
		String sMessage = (sClass == null) ? asMessage : ("[" + sUser + sMethod + "@" + sClass + "] - " + asMessage);

		if (logger == null) {
			System.out.println(logLevel+" "+sMessage);
			return;
		}

		if (logLevel == TRACE && logger.isTraceEnabled())
			logger.debug(sMessage, t);
		if (logLevel == DEBUG && logger.isDebugEnabled())
			logger.debug(sMessage, t);
		if (logLevel == INFO && logger.isInfoEnabled())
			logger.info(sMessage, t);
		if (logLevel == WARNING)
			logger.warn(sMessage, t);
		if (logLevel == ERROR)
			logger.error(sMessage, t);
		if (logLevel == FATAL)
			logger.fatal(sMessage, t);
	}

	// *** DEBUG *** //
	
	public static void debug(String asUser, Object aoCallerObject, String asMethodName, String asMessage) {
		debug(asUser, aoCallerObject, asMethodName, asMessage, null);
	}

	public static void debug(String asUser, Object aoCallerObject, String asMethodName, String asMessage, Throwable t) {
		String stmp = null;
		if (aoCallerObject != null) {
			stmp = aoCallerObject.getClass().getName();
		}
		debug(asUser, stmp, asMethodName, asMessage, t);
	}

	public static void debug(String asUser, String asCallerObject, String asMethodName, String asMessage) {
		log(DEBUG, asUser, asCallerObject, asMethodName, asMessage, null);
	}

	public static void debug(String asUser, String asCallerObject, String asMethodName, String asMessage, Throwable t) {
		log(DEBUG, asUser, asCallerObject, asMethodName, asMessage, t);
	}


	// *** INFO *** //
	
	public static void info(String asUser, Object aoCallerObject, String asMethodName, String asMessage) {
		String stmp = null;
		if (aoCallerObject != null) {
			stmp = aoCallerObject.getClass().getName();
		}
		info(asUser, stmp, asMethodName, asMessage, null);
	}

	public static void info(String asUser, Object aoCallerObject, String asMethodName, String asMessage, Throwable t) {
		String stmp = null;
		if (aoCallerObject != null) {
			stmp = aoCallerObject.getClass().getName();
		}
		info(asUser, stmp, asMethodName, asMessage, t);
	}

	public static void info(String asUser,
			String asCallerObject,
			String asMethodName,
			String asMessage) {
		log(INFO,asUser,asCallerObject,asMethodName,asMessage, null);
	}

	public static void info (String asUser,
			String asCallerObject,
			String asMethodName,
			String asMessage, Throwable t) {
		log(INFO,asUser,asCallerObject,asMethodName,asMessage, t);
	}


	// *** WARNING *** //
	
	public static void warning (String asUser,  Object aoCallerObject, String asMethodName, String asMessage) {
		warning(asUser,aoCallerObject,asMethodName,asMessage, null);
	}

	public static void warning (String asUser, Object aoCallerObject, String asMethodName, String asMessage, Throwable t) {
		String stmp = null;
		if (aoCallerObject != null) {
			stmp = aoCallerObject.getClass().getName();
		}
		warning(asUser,stmp,asMethodName,asMessage, t);
	}

	public static void warning (String asUser, String asCallerObject, String asMethodName, String asMessage) {
		log(WARNING,asUser,asCallerObject,asMethodName,asMessage, null);
	}

	public static void warning (String asUser, String asCallerObject, String asMethodName, String asMessage, Throwable t) {
		log(WARNING,asUser,asCallerObject,asMethodName,asMessage, t);
	}


	// *** ERROR *** //
	
	public static void error (String asUser, Object aoCallerObject, String asMethodName, String asMessage) {
		String stmp = null;
		if (aoCallerObject != null) {
			stmp = aoCallerObject.getClass().getName();
		}
		error(asUser,stmp,asMethodName,asMessage);
	}

	public static void error (String asUser, Object aoCallerObject, String asMethodName, String asMessage, Throwable t) {
		String stmp = null;
		if (aoCallerObject != null) {
			stmp = aoCallerObject.getClass().getName();
		}
		error(asUser,stmp,asMethodName,asMessage, t);
	}

	public static void error (String asUser,
			String asCallerObject,
			String asMethodName,
			String asMessage) {
		log(ERROR,asUser,asCallerObject,asMethodName,asMessage, null);
	}

	public static void error (String asUser,
			String asCallerObject,
			String asMethodName,
			String asMessage,
			Throwable t) {
		log(ERROR,asUser,asCallerObject,asMethodName,asMessage, t);
	}


	// *** FATAL *** //
	
	public static void fatal (String asUser, Object aoCallerObject, String asMethodName, String asMessage) {
		String stmp = null;
		if (aoCallerObject != null) {
			stmp = aoCallerObject.getClass().getName();
		}
		fatal(asUser,stmp,asMethodName,asMessage);
	}

	public static void fatal (String asUser, String asCallerObject, String asMethodName, String asMessage) {
		log(FATAL,asUser,asCallerObject,asMethodName,asMessage, null);
	}

	public static void fatal (String asUser, String asCallerObject, String asMethodName, String asMessage, Throwable t) {
		log(FATAL,asUser,asCallerObject,asMethodName,asMessage, t);
	}

	public static void main (String[] args) {
		debug("####", "####", "####", "####");
		info("####", "####", "####", "####");
		error("####", "####", "####", "####");
	}
}
