/*******************************************************************************
 * Copyright (c) 2011, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/

rwt.protocol.HandlerRegistry.add( "rwt.widgets.Grid", {

  factory : function( properties ) {
    var styleMap = rwt.protocol.HandlerUtil.createStyleMap( properties.style );
    var configMap = {
      appearance : properties.appearance,
      noScroll : styleMap.NO_SCROLL,
      multiSelection : styleMap.MULTI,
      check : styleMap.CHECK,
      // TODO: Remove this check when bug 355408: [Table] Always uses FULL_SELECTION is fixed
      fullSelection : properties.appearance === "table" ? true : styleMap.FULL_SELECTION,
      hideSelection : styleMap.HIDE_SELECTION,
      checkBoxMetrics : properties.checkBoxMetrics,
      selectionPadding : properties.selectionPadding,
      indentionWidth : properties.indentionWidth,
      splitContainer : properties.splitContainer,
      markupEnabled : properties.markupEnabled
    };
    var result = new rwt.widgets.Grid( configMap );
    rwt.protocol.HandlerUtil.addStatesForStyles( result, properties.style );
    result.setUserData( "isControl", true );
    rwt.protocol.HandlerUtil.setParent( result, properties.parent );
    return result;
  },

  destructor : function( widget ) {
    var destroyItems = widget.getRootItem().getUncachedChildren();
    for( var i = 0; i < destroyItems.length; i++ ) {
      destroyItems[ i ].dispose();
    }
    rwt.protocol.HandlerUtil.getControlDestructor()( widget );
  },

  getDestroyableChildren : function( widget ) {
    var result = widget.getRootItem().getCachedChildren();
    return result.concat( rwt.protocol.HandlerUtil.getDestroyableChildrenFinder()( widget ) );
  },

  properties : rwt.protocol.HandlerUtil.extendControlProperties( [
    "itemCount",
    "itemHeight",
    "itemMetrics",
    // NOTE : Client currently requires itemMetrics before columnCount
    "columnCount",
    "treeColumn",
    "fixedColumns",
    "headerHeight",
    "headerVisible",
    "footerHeight",
    "footerVisible",
    "linesVisible",
    "topItemIndex",
    "scrollLeft",
    "selection",
    "focusItem",
    "sortDirection",
    "sortColumn",
    "alwaysHideSelection",
    "enableCellToolTip",
    "cellToolTipText"
  ] ),

  propertyHandler : rwt.protocol.HandlerUtil.extendControlPropertyHandler( {
    "itemMetrics" : function( widget, value ) {
      for( var i = 0; i < value.length; i++ ) {
        widget.setItemMetrics.apply( widget, value[ i ] );
      }
    },
    "fixedColumns" : function( widget, value ) {
      rwt.widgets.util.GridUtil.setFixedColumns( widget, value );
    },
    "focusItem" : function( widget, value ) {
      rwt.protocol.HandlerUtil.callWithTarget( value, function( item ) {
        widget.setFocusItem( item );
      } );
    },
    "selection" : function( widget, value ) {
      widget.deselectAll();
      var applySelection = function( item ) {
        widget.selectItem( item );
      };
      for( var i = 0; i < value.length; i++ ) {
        rwt.protocol.HandlerUtil.callWithTarget( value[ i ], applySelection );
      }
    },
    "sortColumn" : function( widget, value ) {
      rwt.protocol.HandlerUtil.callWithTarget( value, function( column ) {
        widget.setSortColumn( column );
      } );
    },
    "scrollBarsVisible" : function( widget, value ) {
      widget.setScrollBarsVisible( value[ 0 ], value[ 1 ] );
    },
    "cellToolTipText" : function( widget, value ) {
      var EncodingUtil = rwt.protocol.EncodingUtil;
      var text = EncodingUtil.escapeText( value, false );
      text = EncodingUtil.replaceNewLines( text, "<br/>" );
      widget.setCellToolTipText( text );
    }
  } ),

  listeners : rwt.protocol.HandlerUtil.extendControlListeners( [
    "Selection",
    "DefaultSelection",
    "Expand",
    "Collapse",
    "SetData"
  ] ),

  listenerHandler : rwt.protocol.HandlerUtil.extendControlListenerHandler( {
    "scrollBarsSelection" : function( widget, value ) {
      widget.setHasScrollBarsSelectionListener( value );
    }
  } )

} );