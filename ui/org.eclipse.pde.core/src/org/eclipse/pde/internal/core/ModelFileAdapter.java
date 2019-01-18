/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ModelFileAdapter extends FileAdapter {
	private final IPluginModelBase fModel;

	public ModelFileAdapter(IPluginModelBase model, File file, IFileAdapterFactory factory) {
		super(null, file, factory);
		fModel = model;
	}

	public IPluginModelBase getModel() {
		return fModel;
	}
}
