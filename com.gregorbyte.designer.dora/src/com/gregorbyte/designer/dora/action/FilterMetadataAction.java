package com.gregorbyte.designer.dora.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import com.gregorbyte.designer.dora.builder.DoraPostNsfToPhysicalBuilder;
import com.ibm.commons.swt.dialog.LWPDMessageDialog;
import com.ibm.commons.swt.util.EclipseUtils;
import com.ibm.commons.util.StringUtil;
import com.ibm.designer.domino.ide.resources.DominoResourcesPlugin;
import com.ibm.designer.domino.ide.resources.ResourceHandler;
import com.ibm.designer.domino.ide.resources.jni.NotesDesignElement;
import com.ibm.designer.domino.ide.resources.jni.NotesParentAction;
import com.ibm.designer.domino.ide.resources.project.IDominoDesignerProject;
import com.ibm.designer.domino.team.action.AbstractTeamHandler;
import com.ibm.designer.domino.team.builder.ConflictSyncOperation;
import com.ibm.designer.domino.team.builder.ISyncOperation;
import com.ibm.designer.domino.team.builder.ImportSyncOperation;
import com.ibm.designer.domino.team.util.SyncUtil;

public class FilterMetadataAction extends AbstractTeamHandler {

	private IProject diskProject = null;

	ArrayList<NotesParentAction> visitedActions = null;
	ArrayList<IPath> visitedMetadataFiles;
	ArrayList<IPath> visitedContentFiles;
	protected ArrayList<ImportSyncOperation> importSyncs;
	protected ArrayList<ConflictSyncOperation> conflictOps;
	boolean postImportProcessing = false;
	protected ArrayList<ISyncOperation> deferOps;
	public static final int NOT_SET = 0;
	public static final int IMPORT = 1;
	public static final int EXPORT = 2;
	public static final int DETECT = 3;
	public static final int STATUSCODE_DEFER = 36;
	int defaultAction = 0;
	MultiStatus finalStatus;

	public FilterMetadataAction() {
		// SyncUtil.initOnUIThread();

	}

	public void setSyncProjects(IDominoDesignerProject designerProject,
			IProject diskProject) {

		this.desProject = designerProject;
		this.diskProject = diskProject;
		aboutToStart();

	}

	protected void aboutToStart() {

		// TODO not sure how relevant all this is

		this.visitedActions = new ArrayList<NotesParentAction>();
		this.visitedMetadataFiles = new ArrayList<IPath>();
		this.visitedContentFiles = new ArrayList<IPath>();
		this.importSyncs = new ArrayList<ImportSyncOperation>();
		this.conflictOps = new ArrayList<ConflictSyncOperation>();
		this.deferOps = new ArrayList<ISyncOperation>();
		this.finalStatus = new MultiStatus("com.ibm.designer.domino.team", 1,
				ResourceHandler.getString("SyncAction.Error"), null);
		if (this.diskProject == null) {
			try {
				this.diskProject = SyncUtil.getAssociatedDiskProject(
						this.desProject, false);
			} catch (CoreException localCoreException) {
				localCoreException.printStackTrace();
			}
		}

	}

	private void debugPrint(IFile designerFile, IFile diskFile) {

		if (designerFile != null) {
			System.out.println("Designer File: " + designerFile.getName());
			System.out.println("Designer File Mod Stamp: " + designerFile.getModificationStamp());
		}
		if (diskFile != null) {
			System.out.println("Disk File:  " + diskFile.getName());			
			System.out.println("Disk File Mod Stamp: " + diskFile.getModificationStamp());
			
		}

		if (SyncUtil.isModifiedBySync(designerFile)) {
			System.out.println("Designer file Modified By Sync");
		} else {
			System.out.println("Designer file Not Modified By Sync");
		}

		if (SyncUtil.isUsedForSync(designerFile)) {
			System.out.println("Designer file Used For Sync");
		} else {
			System.out.println("Designer file Not Used For Sync");
		}

		if (SyncUtil.isModifiedBySync(diskFile)) {
			System.out.println("Disk file Modified By Sync");
		} else {
			System.out.println("Disk file Not Modified By Sync");
		}

		if (SyncUtil.isUsedForSync(diskFile)) {
			System.out.println("Disk file Used For Sync");
		} else {
			System.out.println("Disk file Not Used For Sync");
		}

		
	}
	
	private void filter(IFile diskFile, Transformer transformer, IProgressMonitor monitor) throws TransformerException, CoreException, IOException {
		
		InputStream is = diskFile.getContents();
		
		Source source = new StreamSource(is);
		
		//File result = new File("V:\\Projects\\DesignerTools\\Result.view");
		
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(baos);
		
		transformer.transform(source, result);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		diskFile.setContents(bais, 0, monitor);

		is.close();
		
		SyncUtil.setModifiedBySync(diskFile);
		
	}
	
	public void performFilter(IFile designerFile, IFile diskFile,
			IProgressMonitor monitor) {

		System.out.println("I would perform filter 1 on " + designerFile.getName());
		
		if (!diskFile.exists()) return;
		
		debugPrint(designerFile, diskFile);
		
		IFile metadataFile = null;
		
		NotesDesignElement designElement = DominoResourcesPlugin.getNotesDesignElement(designerFile);
		
		System.out.println(diskFile.getFileExtension());
		
		if (SyncUtil.hasMetadataFile(designElement)) {
			
			System.out.println("We are looking at " + designElement.getName());
			System.out.println("It needs metadata on export");
			
			IPath localPath = designerFile.getProjectRelativePath().addFileExtension("metadata");
			metadataFile = diskFile.getProject().getFile(localPath);
			
			//metadataFile = diskFile.getProject().getFile(diskFile.getProjectRelativePath().addFileExtension("metadata"));
			
			if (!metadataFile.exists()) {
				metadataFile = null;
			}			
			
		} else {
			metadataFile = diskFile;
		}
		
		TransformerFactory factory = TransformerFactory.newInstance();

		try {
			
			File doraXsl = new File("V:\\Projects\\Budget\\xsl\\DXLClean.xsl");				
			Source xslt = new StreamSource(doraXsl);		
			Transformer transformer = factory.newTransformer(xslt);
			
			if (metadataFile != null) {
				filter(metadataFile, transformer,monitor);
			}
			
			System.out.println("Stop");
			
			debugPrint(designerFile, diskFile);
						
			//SyncUtil.setSyncTimestamp(diskFile, time);
			//SyncUtil.setSyncTimestamp(designerFile, time);
			
			DoraPostNsfToPhysicalBuilder.addMarker2(designerFile, "Hola", -1, IMarker.SEVERITY_INFO);
			
		} catch (TransformerConfigurationException e) {

			String message = e.getMessage();
			DoraPostNsfToPhysicalBuilder.addMarker2(designerFile,"Dora Error " + message, -1, IMarker.SEVERITY_INFO);

		} catch (TransformerException e) {
			String message = e.getMessage();
			DoraPostNsfToPhysicalBuilder.addMarker2(designerFile,"Dora Error " + message, -1, IMarker.SEVERITY_INFO);

		} catch (CoreException e) {
			String message = e.getMessage();
			DoraPostNsfToPhysicalBuilder.addMarker2(designerFile,"Dora Error " + message, -1, IMarker.SEVERITY_INFO);
		} catch (IOException e) {
			String message = e.getMessage();
			DoraPostNsfToPhysicalBuilder.addMarker2(designerFile,"Dora Error " + message, -1, IMarker.SEVERITY_INFO);
		} finally {
			
		}
				
		
		
		
//		ArrayList localArrayList = new ArrayList();
//		ISyncOperation localISyncOperation = getSyncOperation(paramIFile2,
//				paramIFile1);
//		if (localISyncOperation != null) {
//			localArrayList.add(localISyncOperation);
//			if ((localISyncOperation instanceof ImportSyncOperation)) {
//				SyncUtil.logToConsole("adding to importSyncs (b):"
//						+ paramIFile2);
//				this.importSyncs.add((ImportSyncOperation) localISyncOperation);
//			}
//		}
//		if (localArrayList.size() > 0) {
//			executeSyncOps(localArrayList, paramIProgressMonitor, true);
//		}

	}

	public void performFilter(IFolder paramIFolder, IFile paramIFile,
			IProgressMonitor paramIProgressMonitor) {

		System.out.println("I would perform filter 2");

		//
		// ArrayList localArrayList = new ArrayList();
		// ISyncOperation localISyncOperation =
		// handleSharedActionsExt(paramIFile,
		// paramIFolder, this.defaultAction);
		// if (localISyncOperation != null) {
		// localArrayList.add(localISyncOperation);
		// if ((localISyncOperation instanceof ImportSyncOperation)) {
		// SyncUtil.logToConsole("adding to importSyncs (a):" + paramIFile);
		// this.importSyncs.add((ImportSyncOperation) localISyncOperation);
		// }
		// }
		// if (localArrayList.size() > 0) {
		// executeSyncOps(localArrayList, paramIProgressMonitor, false);
		// }

	}

	public void doExecute(IProgressMonitor paramIProgressMonitor) {
		aboutToStart();
		if ((this.diskProject != null) && (this.desProject != null)) {
			if (!this.desProject.isProjectInitialized()) {
				if (!this.desProject.isProjectAccessible()) {
					try {
						this.desProject.markAccessible(null, null);
					} catch (CoreException localCoreException) {
						handleOpenError(localCoreException);
						return;
					}
				}
				WorkspaceJob local2 = new WorkspaceJob(
						ResourceHandler
								.getString("SyncAction.Importsyncoperation")) {
					public IStatus runInWorkspace(
							IProgressMonitor paramAnonymousIProgressMonitor)
							throws CoreException {
						FilterMetadataAction.this
								.performFilter(paramAnonymousIProgressMonitor);
						return Status.OK_STATUS;
					}

					public boolean belongsTo(Object paramAnonymousObject) {
						return super.belongsTo(paramAnonymousObject);
					}

					@SuppressWarnings("rawtypes")
					public Object getAdapter(Class paramAnonymousClass) {
						Object localObject = super
								.getAdapter(paramAnonymousClass);
						if ((paramAnonymousClass != null)
								&& (FilterMetadataAction.this.desProject != null)
								&& (paramAnonymousClass.equals(IResource.class))) {
							localObject = FilterMetadataAction.this.desProject
									.getProject();
						}
						return localObject;
					}
				};
				local2.setUser(true);
				local2.setRule(ResourcesPlugin.getWorkspace().getRuleFactory()
						.buildRule());
				deferScheduleSync(local2);
				return;
			}
			performFilter(paramIProgressMonitor);
		}
	}

	protected void performFilter(IProgressMonitor paramIProgressMonitor) {

		System.out.println("I would perform filter 3");

		// Object localObject;
		// if (paramIProgressMonitor == null) {
		// localObject = new WorkspaceModifyOperation() {
		// protected void execute(
		// IProgressMonitor paramAnonymousIProgressMonitor)
		// throws CoreException, InvocationTargetException,
		// InterruptedException {
		// SyncUtil.logToConsole("performSync: modifyOp");
		// List localList = FilterMetadataAction.this
		// .analyzeSyncOperations(paramAnonymousIProgressMonitor);
		// if (localList.size() > 0) {
		// SyncAction.this.executeSyncOps(localList,
		// paramAnonymousIProgressMonitor, true);
		// }
		// FilterMetadataAction.this
		// .performPostImportProcessing(paramAnonymousIProgressMonitor);
		// FilterMetadataAction.this
		// .handleConflictOps(paramAnonymousIProgressMonitor);
		// FilterMetadataAction.this
		// .scheduleTimestampUpdateJobForImports(SyncAction.this.importSyncs);
		// FilterMetadataAction.this
		// .postProcessing(paramAnonymousIProgressMonitor);
		// }
		// };
		// try {
		// PlatformUI.getWorkbench().getProgressService()
		// .run(true, false, (IRunnableWithProgress) localObject);
		// } catch (InvocationTargetException localInvocationTargetException) {
		// localInvocationTargetException.printStackTrace();
		// } catch (InterruptedException localInterruptedException) {
		// localInterruptedException.printStackTrace();
		// }
		// } else {
		// localObject = analyzeSyncOperations(paramIProgressMonitor);
		// if (((List) localObject).size() > 0) {
		// executeSyncOps((List) localObject, paramIProgressMonitor, true);
		// }
		// SyncUtil.logToConsole("performSync");
		// performPostImportProcessing(paramIProgressMonitor);
		// handleConflictOps(paramIProgressMonitor);
		// scheduleTimestampUpdateJobForImports(this.importSyncs);
		// postProcessing(paramIProgressMonitor);
		// }
	}

	private void handleOpenError(final CoreException paramCoreException) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				LWPDMessageDialog.openError(EclipseUtils.findShell(true),
						paramCoreException);
			}
		});
	}

	private void deferScheduleSync(final WorkspaceJob paramWorkspaceJob) {

		WorkspaceJob local3 = new WorkspaceJob(
				ResourceHandler.getString("SyncAction.Checkingprojectstate")) {
			public IStatus runInWorkspace(
					IProgressMonitor paramAnonymousIProgressMonitor)
					throws CoreException {
				if (!FilterMetadataAction.this.desProject
						.isProjectInitialized()) {
					FilterMetadataAction.this
							.deferScheduleSync(paramWorkspaceJob);
				} else {
					paramWorkspaceJob.schedule();
				}
				return Status.OK_STATUS;
			}
		};
		local3.setUser(false);
		local3.setRule(ResourcesPlugin.getWorkspace().getRuleFactory()
				.buildRule());
		local3.schedule();
	}

}