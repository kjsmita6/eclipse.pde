/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


public class RegistrySearchDialog extends Dialog{
	public static String ENTER_ID = "RegistrySearchDialog.enterId";
	public static String ENTER_NAME = "RegistrySearchDialog.enterName";
	public static String ENTER_VERSION = "RegistrySearchDialog.enterVersion";
	private byte searchType;
	private Text text;
	private String oldText = null;
	
	public RegistrySearchDialog(Shell parentShell, byte type){
		super(parentShell);
		this.searchType = type;
	}
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(container, SWT.NULL);
		if (searchType == RegistrySearchMenu.ID_SEARCH)
			label.setText(PDERuntimePlugin.getResourceString(ENTER_ID));
		else if (searchType == RegistrySearchMenu.NAME_SEARCH)
			label.setText(PDERuntimePlugin.getResourceString(ENTER_NAME));
		else 
			label.setText(PDERuntimePlugin.getResourceString(ENTER_VERSION));
		
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return container;
	}
	
	public int open() {
		text.setText(oldText!=null? oldText : "");
		return super.open();
	}

	protected void okPressed() {
		oldText = text.getText();
		super.okPressed();
	}
	
	public String getSearchText(){
		return oldText;
	}
	
}
