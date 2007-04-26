/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

public class DocumentTextNode implements IDocumentTextNode {
	
	private static final long serialVersionUID = 1L;
	
	private transient int fOffset = -1;
	private transient int fLength = 0;
	private transient IDocumentNode fEnclosingElement;
	
	private String fText;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setEnclosingElement(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setEnclosingElement(IDocumentNode node) {
		fEnclosingElement = node;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getEnclosingElement()
	 */
	public IDocumentNode getEnclosingElement() {
		return fEnclosingElement;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setText(java.lang.String)
	 */
	public void setText(String text) {
		fText = text;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getText()
	 */
	public String getText() {
		return fText == null ? "" : fText; //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getLength()
	 */
	public int getLength() {
		return fLength;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentTextNode#reconnectText(org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void reconnect(IDocumentNode parent) {
		// Transient field:  Enclosing Element
		// Essentially the parent (an element)
		fEnclosingElement = parent;
		// Transient field:  Length
		fLength = -1;
		// Transient field:  Offset
		fOffset = -1;
	}
	
}
