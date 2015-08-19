package com.gregorbyte.designer.dora.builder;

import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.gregorbyte.designer.dora.action.DoraAction;
import com.gregorbyte.designer.dora.action.FilterMetadataAction;
import com.ibm.commons.util.StringUtil;
import com.ibm.designer.domino.ide.resources.DominoResourcesPlugin;
import com.ibm.designer.domino.ide.resources.NsfException;
import com.ibm.designer.domino.ide.resources.ResourceHandler;
import com.ibm.designer.domino.ide.resources.jni.IResourceUpdateListener;
import com.ibm.designer.domino.ide.resources.jni.NResourceUpdateTracker;
import com.ibm.designer.domino.ide.resources.jni.NotesDesignElement;
import com.ibm.designer.domino.ide.resources.project.IDominoDesignerProject;
import com.ibm.designer.domino.preferences.DominoPreferenceManager;
import com.ibm.designer.domino.team.builder.RenameSyncContext;
import com.ibm.designer.domino.team.builder.RenameSyncOperation;
import com.ibm.designer.domino.team.util.SyncUtil;

public class DoraPreNsfToPhysicalBuilder extends IncrementalProjectBuilder
		implements IResourceUpdateListener, IStartup {

	public static final String BUILDER_ID = "com.gregorbyte.designer.dora.DoraPreNsfToPhysicalBuilder";
	private static final String MARKER_TYPE = "com.gregorbyte.designer.dora.xmlProblem";

	IDominoDesignerProject designerProject = null;
	IProject diskProject = null;
	FilterMetadataAction filterAction = null;

	DoraAction doraAction = null;

	public DoraPreNsfToPhysicalBuilder() {

	}

	public DoraPreNsfToPhysicalBuilder(IDominoDesignerProject designerProject) {
		this.designerProject = designerProject;
	}

	public void initialize() {

		if (this.designerProject != null) {
			try {
				this.diskProject = SyncUtil.getAssociatedDiskProject(
						this.designerProject, false);

				if (this.diskProject != null) {
					this.filterAction = new FilterMetadataAction();
					this.filterAction.setSyncProjects(this.designerProject,
							this.diskProject);
					this.doraAction = new DoraAction();
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

	}

	class FilterMetadataVisitor implements IResourceDeltaVisitor {

		private IProgressMonitor monitor = null;

		public FilterMetadataVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		private boolean processAdded(IResourceDelta delta) {
			try {

				System.out.println("Processing Added");

				if ((delta.getResource() instanceof IFolder)) {
					IFolder folder = (IFolder) delta.getResource();

					if (SyncUtil.isSharedAction(folder.getParent()
							.getProjectRelativePath())) {
						DoraPreNsfToPhysicalBuilder.this.updatePhysicalLocation(
								folder, monitor);
						return false;
					}
				} else if (delta.getResource() instanceof IFile) {

					IFile file = (IFile) delta.getResource();

					DoraPreNsfToPhysicalBuilder.this.updatePhysicalLocation(file,
							monitor);

				}

			} catch (CoreException e) {
				e.printStackTrace();
			}
			return true;
		}

		private boolean processChanged(IResourceDelta delta) {

			System.out.println("Processing Changed");

			try {

				if ((delta.getResource() instanceof IFolder)) {
					IFolder folder = (IFolder) delta.getResource();

					if (SyncUtil.isSharedAction(folder.getParent()
							.getProjectRelativePath())) {
						DoraPreNsfToPhysicalBuilder.this.updatePhysicalLocation(
								folder, monitor);
						return false;
					}
				} else if (delta.getResource() instanceof IFile) {

					IFile file = (IFile) delta.getResource();

					if (SyncUtil.isModifiedBySync(file)) {
						System.out.println("was modified by sync -"
								+ file.getName());

						QualifiedName localQualifiedName = new QualifiedName(
								"com.ibm.designer.domino.team", file
										.getProjectRelativePath().toString());
						String tstamp = file.getProject()
								.getPersistentProperty(localQualifiedName);
						System.out.println(tstamp);

						long l = file.getLocalTimeStamp();
						System.out.println(String.valueOf(l));

					} else {
						System.out.println("not modified by sync -"
								+ file.getName());
					}

					if (DoraUtil.shouldFilter(file)) {
						System.out.println("Would have filtered "
								+ file.getName());
						DoraPreNsfToPhysicalBuilder.this.updatePhysicalLocation(
								file, monitor);
					} else {
						System.out.println("Would Not have filtered "
								+ file.getName());
					}

				}

			} catch (CoreException e) {
				e.printStackTrace();
			}
			return true;
		}

		private boolean processRemoved(IResourceDelta delta) {

			System.out.println("Processing Removed");

			IResource localIResource = delta.getResource();
			try {
				if ((localIResource instanceof IFolder)) {
					if (SyncUtil.isSharedAction(localIResource.getParent()
							.getProjectRelativePath())) {
						DoraPreNsfToPhysicalBuilder.this.updatePhysicalLocation(
								localIResource, monitor);
						return false;
					}
				} else if (localIResource.getType() == 1) {
					IFile localIFile = (IFile) delta.getResource();
					DoraPreNsfToPhysicalBuilder.this.updatePhysicalLocation(
							localIFile, monitor);
				}
			} catch (CoreException localCoreException3) {
				localCoreException3.printStackTrace();
			}
			return true;

		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {

			System.out.println("Visiting: " + delta.getResource().getName());

			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				if (!processAdded(delta)) {
					return false;
				}
				break;
			case IResourceDelta.CHANGED:
				if (!processChanged(delta)) {
					return false;
				}
				break;
			case IResourceDelta.REMOVED:
				if (!processRemoved(delta)) {
					return false;
				}
				break;

			}

			return true;
		}

	}

	class XMLErrorHandler extends DefaultHandler {

		private IFile file;

		public XMLErrorHandler(IFile file) {
			this.file = file;
		}

		private void addMarker(SAXParseException e, int severity) {
			DoraPreNsfToPhysicalBuilder.this.addMarker(file, e.getMessage(),
					e.getLineNumber(), severity);
		}

		public void error(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		public void fatalError(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		public void warning(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_WARNING);
		}
	}

	private SAXParserFactory parserFactory;

	public static void addMarker2(IFile file, String message, int lineNumber,
			int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	private void addMarker(IFile file, String message, int lineNumber,
			int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	@SuppressWarnings("rawtypes")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {

		System.out.println("Dora: PreNsfToPhysicalBuilder");

		return null;
		

	}

	private boolean isRelevant(IResourceDelta delta) throws CoreException {
		final boolean[] arrayOfBoolean = new boolean[1];
		delta.accept(new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta paramAnonymousIResourceDelta) {
				switch (paramAnonymousIResourceDelta.getKind()) {
				case 1:
					if ((paramAnonymousIResourceDelta.getResource() instanceof IFile)) {
						arrayOfBoolean[0] = true;
						return false;
					}
					break;
				case 4:
					if ((paramAnonymousIResourceDelta.getResource() instanceof IFile)) {
						arrayOfBoolean[0] = true;
						return false;
					}
					break;
				case 2:
					IResource localIResource = paramAnonymousIResourceDelta
							.getResource();
					if (localIResource.getType() == 1) {
						arrayOfBoolean[0] = true;
						return false;
					}
					break;
				}
				return true;
			}
		});
		return arrayOfBoolean[0];
	}

	public void updatePhysicalLocation(IResource resource,
			IProgressMonitor monitor) throws CoreException {

		IFile file = SyncUtil.getPhysicalFile(this.designerProject, resource);

		if (resource instanceof IFile) {

			if (DoraUtil.isModifiedBySync(resource)) {
				System.out.println("Dora says: Modified by sync");
			} else {
				System.out.println("Dora says: Not Modified by sync");
			}

			System.out.println("timestamp is x");

			this.doraAction.designerProject = this.designerProject;

			DoraPreNsfToPhysicalBuilder.this.doraAction.getSyncOperation(
					(IFile) resource, file);

			this.filterAction.performFilter((IFile) resource, file, monitor);
		} else {
			this.filterAction.performFilter((IFolder) resource, file, monitor);
		}
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		// delete markers set and files created
		getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
	}

	void checkXML(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".xml")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);
			XMLErrorHandler reporter = new XMLErrorHandler(file);
			try {
				getParser().parse(file.getContents(), reporter);
			} catch (Exception e1) {
			}
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	private SAXParser getParser() throws ParserConfigurationException,
			SAXException {
		if (parserFactory == null) {
			parserFactory = SAXParserFactory.newInstance();
		}
		return parserFactory.newSAXParser();
	}

	@Override
	public void earlyStartup() {

		NResourceUpdateTracker.getInstance().addListener(this);

	}

	@Override
	public void designerProjectInitialized(
			final IDominoDesignerProject paramIDominoDesignerProject) {

		if (!SyncUtil.isSourceControlEnabled() || (!isAutoExportEnabled())) {
			return;
		}

		if ((paramIDominoDesignerProject == null)
				|| (!SyncUtil
						.isConfiguredForSourceControl(paramIDominoDesignerProject))) {
			return;
		}

		WorkspaceJob local5 = new WorkspaceJob(
				ResourceHandler
						.getString("NsfToPhysicalSynBuilder.SyncingNSFtophysicalproject.1")) {
			public IStatus runInWorkspace(
					IProgressMonitor paramAnonymousIProgressMonitor)
					throws CoreException {
				IProject localIProject = null;
				if (paramIDominoDesignerProject != null) {
					try {
						localIProject = SyncUtil.getAssociatedDiskProject(
								paramIDominoDesignerProject, false);
						if ((localIProject != null) && (localIProject.exists())) {
							FilterMetadataAction filterAction = new FilterMetadataAction();
							filterAction.setSyncProjects(
									paramIDominoDesignerProject, localIProject);
							filterAction
									.doExecute(paramAnonymousIProgressMonitor);
						}
					} catch (CoreException localCoreException) {
						localCoreException.printStackTrace();
					}
				}
				return Status.OK_STATUS;
			}
		};
		local5.setRule(ResourcesPlugin.getWorkspace().getRoot());
		local5.schedule();
	}

	@Override
	public void nestedResourceUpdated(IDominoDesignerProject designerProject,
			final IResource resource, final long time) {

		if (!SyncUtil.isSourceControlEnabled()) {
			return;
		}
		if ((designerProject == null)
				|| (!SyncUtil.isConfiguredForSourceControl(designerProject))) {
			return;
		}
		WorkspaceJob local4 = new WorkspaceJob(
				ResourceHandler
						.getString("NsfToPhysicalSynBuilder.SyncingNSFtophysicalproject.1")) {
			public IStatus runInWorkspace(
					IProgressMonitor paramAnonymousIProgressMonitor)
					throws CoreException {
				boolean bool1 = SyncUtil.isUsedForSync(resource);
				boolean bool2 = SyncUtil.isModifiedBySync(resource);
				if ((bool1) && (bool2)) {
					SyncUtil.setSyncTimestamp(resource, time);
				}
				return Status.OK_STATUS;
			}
		};
		local4.setRule(designerProject.getProject());
		local4.schedule();

	}

	@Override
	public void resourceModified(IDominoDesignerProject designerProject) {

	}

	@SuppressWarnings("unused")
	private boolean isAutoImportEnabled() {
		return DominoPreferenceManager.getInstance().getBooleanValue(
				"domino.prefs.keys.team.import.auto", false);
	}

	public boolean isAutoExportEnabled() {
		return DominoPreferenceManager.getInstance().getBooleanValue(
				"domino.prefs.keys.team.export.auto", false);
	}

	@Override
	public void resourceRenamed(
			final IDominoDesignerProject paramIDominoDesignerProject,
			final IResource paramIResource1, final IResource paramIResource2) {

		if ((!SyncUtil.isSourceControlEnabled()) || (!isAutoExportEnabled())) {
			return;
		}
		if ((paramIDominoDesignerProject == null)
				|| (!SyncUtil
						.isConfiguredForSourceControl(paramIDominoDesignerProject))
				|| (!paramIDominoDesignerProject.isProjectInitialized())) {
			return;
		}
		WorkspaceJob local3 = new WorkspaceJob(
				ResourceHandler
						.getString("NsfToPhysicalSynBuilder.SyncingNSFtophysicalproject.1")) {
			public IStatus runInWorkspace(
					IProgressMonitor paramAnonymousIProgressMonitor)
					throws CoreException {
				Object localObject1 = (paramIResource1 instanceof IFile) ? SyncUtil
						.getPhysicalFile(paramIDominoDesignerProject,
								paramIResource1) : SyncUtil.getPhysicalFolder(
						paramIDominoDesignerProject, paramIResource1);
				Object localObject2 = (paramIResource2 instanceof IFile) ? SyncUtil
						.getPhysicalFile(paramIDominoDesignerProject,
								paramIResource2) : SyncUtil.getPhysicalFolder(
						paramIDominoDesignerProject, paramIResource2);
				if (((paramIResource2 instanceof IFile))
						&& (!SyncUtil.canSync((IFile) paramIResource2,
								(IFile) localObject2,
								paramIDominoDesignerProject))) {
					return Status.OK_STATUS;
				}
				NotesDesignElement localNotesDesignElement = DominoResourcesPlugin
						.getNotesDesignElement(paramIResource1);
				if ((localNotesDesignElement != null)
						&& (StringUtil.equals(
								localNotesDesignElement.getMetaModelID(),
								"metamodel.sharedactions"))) {
					localObject1 = SyncUtil.getPhysicalFile(
							paramIDominoDesignerProject, paramIResource1);
					localObject2 = SyncUtil.getPhysicalFile(
							paramIDominoDesignerProject, paramIResource2);
				}
				RenameSyncOperation localRenameSyncOperation = new RenameSyncOperation();
				localRenameSyncOperation.initContext(new RenameSyncContext(
						paramIDominoDesignerProject, paramIResource1,
						(IResource) localObject1, paramIResource2,
						(IResource) localObject2));
				return localRenameSyncOperation
						.performSync(paramAnonymousIProgressMonitor);
			}
		};
		local3.setRule(ResourcesPlugin.getWorkspace().getRoot());
		local3.schedule();

	}

}