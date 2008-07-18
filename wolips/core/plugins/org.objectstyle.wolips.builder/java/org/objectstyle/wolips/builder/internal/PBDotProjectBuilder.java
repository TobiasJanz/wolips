/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2005 - 2006 The ObjectStyle Group,
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ========================================================
 * ============
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.wolips.builder.internal;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.objectstyle.wolips.core.resources.builder.IFullBuilder;
import org.objectstyle.wolips.core.resources.builder.IIncrementalBuilder;
import org.objectstyle.wolips.core.resources.types.ILocalizedPath;
import org.objectstyle.wolips.core.resources.types.IPBDotProjectOwner;
import org.objectstyle.wolips.core.resources.types.file.IPBDotProjectAdapter;
import org.objectstyle.wolips.core.resources.types.folder.IDotEOModeldAdapter;
import org.objectstyle.wolips.core.resources.types.folder.IDotSubprojAdapter;
import org.objectstyle.wolips.core.resources.types.folder.IDotWoAdapter;
import org.objectstyle.wolips.core.resources.types.project.IProjectAdapter;
import org.objectstyle.wolips.jdt.ProjectFrameworkAdapter;
import org.objectstyle.wolips.preferences.Preferences;

public class PBDotProjectBuilder implements IIncrementalBuilder, IFullBuilder {
	private Hashtable affectedPBDotProjectOwner;

	public PBDotProjectBuilder() {
		super();
	}

	private String key(IResource resource) {
		return resource.getLocation().toPortableString();
	}

	private IPBDotProjectAdapter getIPBDotProjectAdapterForKey(IResource resource) {
		String key = this.key(resource);
		if (affectedPBDotProjectOwner.containsKey(key)) {
			return (IPBDotProjectAdapter) affectedPBDotProjectOwner.get(key);
		}
		return null;
	}

	private void setIPBDotProjectOwnerForKey(IPBDotProjectAdapter pbDotProjectAdapter, IResource resource) {
		affectedPBDotProjectOwner.put(this.key(resource), pbDotProjectAdapter);
	}

	public boolean buildStarted(int kind, Map args, IProgressMonitor monitor, IProject project, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return false;
		}
		this.affectedPBDotProjectOwner = new Hashtable();
		IProjectAdapter projectAdapter = (IProjectAdapter) project.getAdapter(IProjectAdapter.class);
		IPBDotProjectAdapter adapter = projectAdapter.getPBDotProjectAdapter();
		boolean fullBuildRequested = adapter.isRebuildRequired();
		return fullBuildRequested;
	}

	public boolean buildPreparationDone(int kind, Map args, IProgressMonitor monitor, IProject project, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return false;
		}
		Iterator iterator = affectedPBDotProjectOwner.values().iterator();
		while (iterator.hasNext()) {
			Object object = iterator.next();
			IPBDotProjectAdapter pbDotProjectAdapter = (IPBDotProjectAdapter) object;
			pbDotProjectAdapter.save();
		}
		this.affectedPBDotProjectOwner = null;
		return false;
	}

	private IPBDotProjectOwner getIPBDotProjectOwner(IResource resource) {
		IProject project = resource.getProject();
		IProjectAdapter projectAdapter = (IProjectAdapter) project.getAdapter(IProjectAdapter.class);
		IPBDotProjectOwner pbDotProjectOwner = projectAdapter.getPBDotProjectOwner(resource);
		return pbDotProjectOwner;
	}

	public IPBDotProjectAdapter getIPBDotProjectAdapter(IPBDotProjectOwner pbDotProjectOwner) {
		IPBDotProjectAdapter pbDotProjectAdapter = this.getIPBDotProjectAdapterForKey(pbDotProjectOwner.getUnderlyingResource());
		if (pbDotProjectAdapter == null) {
			pbDotProjectAdapter = pbDotProjectOwner.getPBDotProjectAdapter();
			this.setIPBDotProjectOwnerForKey(pbDotProjectAdapter, pbDotProjectOwner.getUnderlyingResource());
			// pbDotProjectAdapter.cleanTables();
		}
		return pbDotProjectAdapter;
	}

	public void handleSourceDelta(IResourceDelta delta, IProgressMonitor monitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		IResource resource = delta.getResource();
		handleSource(delta.getKind(), resource);
	}

	public void handleSource(IResource resource, IProgressMonitor progressMonitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		handleSource(IResourceDelta.ADDED, resource);
	}

	private boolean handleSource(int kind, IResource resource) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return false;
		}
		if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED || kind == IResourceDelta.REMOVED) {
			IPBDotProjectOwner pbDotProjectOwner = this.getIPBDotProjectOwner(resource);
			IPBDotProjectAdapter pbDotProjectAdapter = this.getIPBDotProjectAdapter(pbDotProjectOwner);
			ILocalizedPath localizedPath = pbDotProjectAdapter.localizedRelativeResourcePath(pbDotProjectOwner, resource);
			if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED) {
				pbDotProjectAdapter.addClass(localizedPath);
			} else if (kind == IResourceDelta.REMOVED) {
				pbDotProjectAdapter.removeClass(localizedPath);
			}
		}
		return false;
	}

	public void handleClassesDelta(IResourceDelta delta, IProgressMonitor monitor, Map buildCache) {
		// do nothing
	}

	public void handleClasses(IResource resource, IProgressMonitor progressMonitor, Map buildCache) {
		// do nothing
	}

	public void handleWoappResourcesDelta(IResourceDelta delta, IProgressMonitor monitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		handleWoappResources(delta.getKind(), delta.getResource());
	}

	public void handleWoappResources(IResource resource, IProgressMonitor progressMonitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		handleWoappResources(IResourceDelta.ADDED, resource);
	}

	public void handleWoappResources(int kind, IResource resource) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED || kind == IResourceDelta.REMOVED) {
			IPBDotProjectOwner pbDotProjectOwner = this.getIPBDotProjectOwner(resource);
			IPBDotProjectAdapter pbDotProjectAdapter = this.getIPBDotProjectAdapter(pbDotProjectOwner);
			ILocalizedPath localizedPath = pbDotProjectAdapter.localizedRelativeResourcePath(pbDotProjectOwner, resource);
			IDotWoAdapter dotWoAdapter = (IDotWoAdapter) resource.getAdapter(IDotWoAdapter.class);
			boolean isDotWO = dotWoAdapter != null;
			IDotWoAdapter parentWoAdapter = null;
			if (resource.getParent() != null) {
				parentWoAdapter = (IDotWoAdapter) resource.getParent().getAdapter(IDotWoAdapter.class);
			}
			boolean parentIsDotWO = parentWoAdapter != null;
			if (parentIsDotWO) {
				return;
			}
			IDotEOModeldAdapter parentDotEOModeldAdapter = null;
			if (resource.getParent() != null) {
				parentDotEOModeldAdapter = (IDotEOModeldAdapter) resource.getParent().getAdapter(IDotEOModeldAdapter.class);
			}
			boolean parentIsDotEOModeld = parentDotEOModeldAdapter != null;
			if (parentIsDotEOModeld) {
				return;
			}
			if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED) {
				if (isDotWO) {
					pbDotProjectAdapter.addWoComponent(localizedPath);
				} else {
					pbDotProjectAdapter.addWoappResource(localizedPath);
				}
			} else if (kind == IResourceDelta.REMOVED) {
				if (isDotWO) {
					pbDotProjectAdapter.removeWoComponent(localizedPath);
				} else {
					pbDotProjectAdapter.removeWoappResource(localizedPath);
				}
			}
		}
	}

	public void handleWebServerResources(IResource resource, IProgressMonitor progressMonitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		handleWebServerResources(IResourceDelta.ADDED, resource);
	}

	public void handleWebServerResourcesDelta(IResourceDelta delta, IProgressMonitor monitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		IResource resource = delta.getResource();
		handleWebServerResources(delta.getKind(), resource);
	}

	private void handleWebServerResources(int kind, IResource resource) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED || kind == IResourceDelta.REMOVED) {
			IPBDotProjectOwner pbDotProjectOwner = this.getIPBDotProjectOwner(resource);
			IPBDotProjectAdapter pbDotProjectAdapter = this.getIPBDotProjectAdapter(pbDotProjectOwner);
			ILocalizedPath localizedPath = pbDotProjectAdapter.localizedRelativeResourcePath(pbDotProjectOwner, resource);
			if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED) {
				pbDotProjectAdapter.addWebServerResource(localizedPath);
			} else if (kind == IResourceDelta.REMOVED) {
				pbDotProjectAdapter.removeWebServerResource(localizedPath);
			}
		}
	}

	public void handleOther(IResource resource, IProgressMonitor monitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		handleOther(IResourceDelta.ADDED, resource);
	}

	public void handleOtherDelta(IResourceDelta delta, IProgressMonitor monitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		IResource resource = delta.getResource();
		handleOther(delta.getKind(), resource);
	}

	private void handleOther(int kind, IResource resource) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		IDotSubprojAdapter dotSubprojAdapter = (IDotSubprojAdapter) resource.getAdapter(IDotSubprojAdapter.class);
		if (dotSubprojAdapter == null) {
			return;
		}
		IPBDotProjectOwner pbDotProjectOwner = this.getIPBDotProjectOwner(resource.getParent());
		IPBDotProjectAdapter pbDotProjectAdapter = this.getIPBDotProjectAdapter(pbDotProjectOwner);
		if (kind == IResourceDelta.ADDED) {
			pbDotProjectAdapter.addSubproject(dotSubprojAdapter);
		}
		if (kind == IResourceDelta.REMOVED) {
			pbDotProjectAdapter.removeSubproject(dotSubprojAdapter);
		}
	}

	public void handleClasspath(IResource resource, IProgressMonitor progressMonitor, Map buildCache) {

		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		IPBDotProjectOwner pbDotProjectOwner = this.getIPBDotProjectOwner(resource);
		IPBDotProjectAdapter pbDotProjectAdapter = this.getIPBDotProjectAdapter(pbDotProjectOwner);
		IProject project = resource.getProject();
		ProjectFrameworkAdapter projectAdapter = (ProjectFrameworkAdapter) project.getAdapter(ProjectFrameworkAdapter.class);
		try {
			Set<String> frameworkNames = projectAdapter.getFrameworkNames();
			pbDotProjectAdapter.updateFrameworkNames(new LinkedList<String>(frameworkNames));
		} catch (JavaModelException e) {
			throw new RuntimeException("Unable to retrieve a list of frameworks.", e);
		}
	}

	public void classpathChanged(IResourceDelta delta, IProgressMonitor monitor, Map buildCache) {
		if (!Preferences.shouldWritePBProjOnBuild()) {
			return;
		}
		IResource resource = delta.getResource();
		handleClasspath(resource, monitor, buildCache);
	}

}
