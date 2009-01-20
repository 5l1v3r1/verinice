package sernet.gs.ui.rcp.main.bsi.dialogs;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.property.Setter;

import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.bsi.editors.InputHelperFactory;
import sernet.hui.common.connect.Entity;
import sernet.hui.common.connect.EntityType;
import sernet.hui.common.connect.HUITypeFactory;
import sernet.hui.common.connect.Property;
import sernet.hui.common.connect.PropertyList;
import sernet.hui.swt.widgets.HitroUIComposite;
import sernet.snutils.DBException;
import sernet.snutils.ExceptionHandlerFactory;

public class BulkEditDialog extends Dialog {

	private EntityType entType;
	private Entity entity = null;
	private boolean useRules = false;
	private String title = "Bulk Edit";
	
	public BulkEditDialog(Shell parent,
			EntityType entType) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		this.entType = entType;
	}
	
	public BulkEditDialog(Shell shell, EntityType entType2, boolean b, String title, Entity entity) {
		this(shell, entType2);
		useRules = true;
		this.title = title;
		this.entity = entity;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
		newShell.setSize(400, 800);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		try {
			Composite container = (Composite) super.createDialogArea(parent);
			container.setLayout(new FillLayout());
			
			HitroUIComposite huiComposite 
				= new HitroUIComposite(container, SWT.NULL, false);
			
			try {
				if (this.entity == null)
					entity = new Entity(entType.getId());
				huiComposite.createView(entity, true, useRules);
				InputHelperFactory.setInputHelpers(entType, huiComposite);
				return huiComposite;
			} catch (DBException e) {
				ExceptionUtil.log(e, "Fehler beim Anzeigen des Bulk Edit Dialogs.");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Entity getEntity() {
		return entity;
	}
		
}
