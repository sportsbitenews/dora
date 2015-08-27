package com.gregorbyte.designer.dora.builder.post;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.gregorbyte.designer.dora.util.DoraUtil;
import com.ibm.designer.domino.ide.resources.project.IDominoDesignerProject;
import com.ibm.designer.domino.team.util.SyncUtil;

public class SwiperPostVisitor implements IResourceDeltaVisitor {

	private IProgressMonitor monitor = null;
	private SwiperPostSyncBuilder builder = null;
	private IDominoDesignerProject designerProject = null;

	public SwiperPostVisitor(IProgressMonitor monitor,
			SwiperPostSyncBuilder builder) {
		this.monitor = monitor;
		this.builder = builder;
		this.designerProject = builder.getDesignerProject();
	}

	private boolean processAdded(IResourceDelta delta) {
		try {

			DoraUtil.logInfo("Processing Added");

			if ((delta.getResource() instanceof IFolder)) {
				IFolder folder = (IFolder) delta.getResource();

				if (SyncUtil.isSharedAction(folder.getParent()
						.getProjectRelativePath())) {
					builder.updatePhysicalLocation(folder, monitor);
					return false;
				}
			} else if (delta.getResource() instanceof IFile) {

				IFile file = (IFile) delta.getResource();

				builder.updatePhysicalLocation(file, monitor);

			}

		} catch (CoreException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean processChanged(IResourceDelta delta) {

		DoraUtil.logInfo("Processing Changed");

		try {

			if ((delta.getResource() instanceof IFolder)) {
				IFolder folder = (IFolder) delta.getResource();

				if (SyncUtil.isSharedAction(folder.getParent()
						.getProjectRelativePath())) {
					builder.updatePhysicalLocation(folder, monitor);
					return false;
				}
			} else if (delta.getResource() instanceof IFile) {

				IFile designerFile = (IFile) delta.getResource();

				if (designerFile.getName().contains("ccCustomControl.xsp")){
					System.out.println(designerFile.getName());
				}
				
				IFile diskFile = DoraUtil.getRelevantDiskFile(designerProject,
						designerFile);

				if (diskFile != null && diskFile.exists()) {

					if (DoraUtil.isModifiedBySync(diskFile)) {

						DoraUtil.logInfo(diskFile.getName()
								+ " was modified by sync - Filter It");

						if (DoraUtil.shouldFilter(designerFile)) {

							builder.filterDiskFile(designerFile, diskFile,
									monitor);

						} else {
							DoraUtil.logInfo("Not Configured to filter "
									+ designerFile.getName());
						}

					} else {
						DoraUtil.logInfo(diskFile.getName() + " untouched");
					}

				}

				// if (DoraUtil.shouldFilter(file)) {
				// DoraUtil.logInfo("Would have filtered "
				// + file.getName());
				// DoraPostNsfToPhysicalBuilder.this.updatePhysicalLocation(
				// file, monitor);
				// } else {
				// DoraUtil.logInfo("Would Not have filtered "
				// + file.getName());
				// }

			}

		} catch (CoreException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {

		DoraUtil.logInfo("Visiting: " + delta.getResource().getName());

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

		}

		return true;
	}

}