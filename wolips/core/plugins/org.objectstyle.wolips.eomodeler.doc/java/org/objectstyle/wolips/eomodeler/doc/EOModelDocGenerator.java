package org.objectstyle.wolips.eomodeler.doc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.objectstyle.wolips.eomodeler.core.model.EOEntity;
import org.objectstyle.wolips.eomodeler.core.model.EOModel;
import org.objectstyle.wolips.eomodeler.core.model.EOModelGroup;
import org.objectstyle.wolips.eomodeler.core.model.EOModelVerificationFailure;
import org.objectstyle.wolips.eomodeler.core.model.EOStoredProcedure;

public class EOModelDocGenerator {
	public static class ConsoleLogger implements LogSystem {
		public void init(RuntimeServices runtimeservices) throws Exception {
			// DO NOTHING
		}

		public void logVelocityMessage(int i, String s) {
			System.out.println("ConsoleLogger.logVelocityMessage: " + i + ", " + s);
		}
	}

	public static void generate(EOModelGroup modelGroup, File outputFolder, File templatePath) throws Exception {
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, org.apache.velocity.runtime.log.NullLogSystem.class.getName());
		//velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, ConsoleLogger.class.getName());
		velocityEngine.setProperty("resource.loader", "file,class");
		StringBuffer templatePaths = new StringBuffer();
		templatePaths.append(".");
		if (templatePath != null) {
			templatePaths.append(",");
			templatePaths.append(templatePath.getAbsolutePath());
		}
		velocityEngine.setProperty("resource.loader", "file,class");
		velocityEngine.setProperty("file.resource.loader", templatePaths.toString());
		velocityEngine.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());

		velocityEngine.init();
		VelocityContext context = new VelocityContext();

		context.put("modelGroup", modelGroup);
		EOModelDocGenerator.writeTemplate(velocityEngine, context, "eomodeldoc.css.vm", new File(outputFolder, "eomodeldoc.css"));
		EOModelDocGenerator.writeTemplate(velocityEngine, context, "prototype.js.vm", new File(outputFolder, "prototype.js"));
		EOModelDocGenerator.writeTemplate(velocityEngine, context, "index.html.vm", new File(outputFolder, "index.html"));
		EOModelDocGenerator.writeTemplate(velocityEngine, context, "indexContent.html.vm", new File(outputFolder, "content.html"));
		EOModelDocGenerator.writeTemplate(velocityEngine, context, "indexOverview.html.vm", new File(outputFolder, "overview.html"));
		EOModelDocGenerator.writeTemplate(velocityEngine, context, "indexModels.html.vm", new File(outputFolder, "models.html"));
		for (EOModel model : modelGroup.getModels()) {
			System.out.println("Generating " + model.getName() + " ...");
			context.put("model", model);
			EOModelDocGenerator.writeTemplate(velocityEngine, context, "modelOverview.html.vm", new File(outputFolder, model.getName() + "/overview.html"));
			EOModelDocGenerator.writeTemplate(velocityEngine, context, "modelContent.html.vm", new File(outputFolder, model.getName() + "/content.html"));

			for (EOEntity entity : model.getEntities()) {
				System.out.println("Generating " + model.getName() + "." + entity.getName() + " ...");
				context.put("entity", entity);
				EOModelDocGenerator.writeTemplate(velocityEngine, context, "entityContent.html.vm", new File(outputFolder, model.getName() + "/entities/" + entity.getName() + ".html"));
			}

			for (EOStoredProcedure storedProcedure : model.getStoredProcedures()) {
				System.out.println("Generating " + model.getName() + "." + storedProcedure.getName() + " ...");
				context.put("storedProcedure", storedProcedure);
				EOModelDocGenerator.writeTemplate(velocityEngine, context, "storedProcedureContent.html.vm", new File(outputFolder, model.getName() + "/storedProcedures/" + storedProcedure.getName() + ".html"));
			}
		}
		
		System.out.println("Done: " + new File(outputFolder, "index.html"));
	}

	public static void main(String[] args) throws Exception {
		// String userHomeWOLipsPath = System.getProperty("user.home") +
		// File.separator + "Library" + File.separator + "WOLips";
		// URL url = null;
		// url = FileLocator.resolve(Activator.getDefault().getBundle().);
		// String templatePaths = userHomeWOLipsPath + ", ";
		// Path path = new Path(url.getPath());
		// templatePaths = templatePaths +
		// path.append("templates").toOSString();
		// velocityEngine.setProperty("resource.loader", "wolips");
		// velocityEngine.setProperty("wolips.resource.loader.class",
		// org.objectstyle.wolips.thirdparty.velocity.resourceloader.ResourceLoader.class.getName());
		// velocityEngine.setProperty("wolips.resource.loader.bundle",
		// Activator.getDefault().getBundle());
		// velocityEngine.setProperty("jar.resource.loader.path", "jar:" +
		// TemplateEnginePlugin.getDefault().getBundle().getResource("plugin.xml").getFile());
		EOModelGroup modelGroup = new EOModelGroup();
		modelGroup.loadModelFromURL(new File("/Users/mschrag/Documents/workspace/ERPrototypes/Resources/erprototypes.eomodeld").toURL());
		modelGroup.loadModelFromURL(new File("/Users/mschrag/Documents/workspace/MDTAccounting/MDTAccounting.eomodeld").toURL());
		modelGroup.loadModelFromURL(new File("/Users/mschrag/Documents/workspace/MDTask/MDTask.eomodeld").toURL());
		modelGroup.resolve(new HashSet<EOModelVerificationFailure>());

		File outputFolder = new File("/tmp/eomodeldoc");

		EOModelDocGenerator.generate(modelGroup, outputFolder, null);
	}

	public static void writeTemplate(VelocityEngine engine, VelocityContext context, String templateName, File outputFile) throws ResourceNotFoundException, ParseErrorException, MethodInvocationException, Exception {
		Template template = engine.getTemplate(templateName);
		if (!outputFile.getParentFile().exists()) {
			if (!outputFile.getParentFile().mkdirs()) {
				throw new IOException("Unable to create the folder " + outputFile.getParentFile() + ".");
			}
		}
		FileWriter outputWriter = new FileWriter(outputFile);
		try {
			template.merge(context, outputWriter);
		} finally {
			outputWriter.close();
		}
	}
}
