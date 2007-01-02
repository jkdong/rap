/*******************************************************************************
 * Copyright (c) 2002-2006 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 ******************************************************************************/

package org.eclipse.rap.rwt.internal.widgets.toolitemkit;

import java.io.IOException;
import org.eclipse.rap.rwt.events.SelectionEvent;
import org.eclipse.rap.rwt.internal.widgets.ItemLCAUtil;
import org.eclipse.rap.rwt.internal.widgets.Props;
import org.eclipse.rap.rwt.lifecycle.*;
import org.eclipse.rap.rwt.widgets.ToolItem;

final class CheckToolItemDelegateLCA extends ToolItemDelegateLCA {

  private static final String SELECTED_ITEM = "selectedItem"; 
  // check functions as defined in org.eclipse.rap.rwt.ButtonUtil
  private static final String WIDGET_SELECTED = 
    "org.eclipse.rap.rwt.ButtonUtil.checkSelected";
  // tool item functions as defined in org.eclipse.rap.rwt.ToolItemUtil
  private static final String CREATE_CHECK = 
    "org.eclipse.rap.rwt.ToolItemUtil.createToolItemCheckUtil";
  private final JSListenerInfo JS_LISTENER_INFO = 
    new JSListenerInfo( JSConst.QX_EVENT_CHANGE_CHECKED,
                        WIDGET_SELECTED,
                        JSListenerType.ACTION );

  void readData( final ToolItem toolItem ) {
    if( WidgetUtil.wasEventSent( toolItem, JSConst.EVENT_WIDGET_SELECTED ) ) {
      String value = WidgetUtil.readPropertyValue( toolItem, SELECTED_ITEM );
      toolItem.setSelection( new Boolean( value ).booleanValue() );
      processSelection( toolItem );
    }
  }

  void renderInitialization( final ToolItem toolItem ) throws IOException {
    JSWriter writer = JSWriter.getWriterFor( toolItem );
    Object[] args = new Object[] {
      WidgetUtil.getId( toolItem ),
      toolItem.getParent()
    };
    writer.callStatic( CREATE_CHECK, args );
    writer.set( "checked", toolItem.getSelection() );
  }

  void renderChanges( final ToolItem toolItem ) throws IOException {
    JSWriter writer = JSWriter.getWriterFor( toolItem );
    ItemLCAUtil.writeChanges( toolItem );
    // TODO [rh] the JSConst.JS_WIDGET_SELECTED does unnecessarily send
    // bounds of the widget that was clicked -> In the SelectionEvent
    // for ToolItem the bounds are undefined
    writer.updateListener( JS_LISTENER_INFO,
                           Props.SELECTION_LISTENERS,
                           SelectionEvent.hasListener( toolItem ) );
  }
}
