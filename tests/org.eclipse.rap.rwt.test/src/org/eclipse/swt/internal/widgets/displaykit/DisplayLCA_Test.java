/*******************************************************************************
 * Copyright (c) 2002, 2014 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 *    Frank Appel - replaced singletons and static fields (Bug 337787)
 ******************************************************************************/
package org.eclipse.swt.internal.widgets.displaykit;

import static org.eclipse.rap.rwt.internal.lifecycle.DisplayUtil.getAdapter;
import static org.eclipse.rap.rwt.internal.lifecycle.DisplayUtil.getId;
import static org.eclipse.rap.rwt.internal.lifecycle.WidgetUtil.getAdapter;
import static org.eclipse.rap.rwt.internal.lifecycle.WidgetUtil.getId;
import static org.eclipse.rap.rwt.internal.protocol.JsonUtil.createJsonArray;
import static org.eclipse.rap.rwt.internal.service.ContextProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;

import org.eclipse.rap.json.JsonObject;
import org.eclipse.rap.json.JsonValue;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.client.service.ExitConfirmation;
import org.eclipse.rap.rwt.internal.application.ApplicationContextImpl;
import org.eclipse.rap.rwt.internal.lifecycle.AbstractWidgetLCA;
import org.eclipse.rap.rwt.internal.lifecycle.DisplayUtil;
import org.eclipse.rap.rwt.internal.lifecycle.LifeCycleUtil;
import org.eclipse.rap.rwt.internal.lifecycle.PhaseEvent;
import org.eclipse.rap.rwt.internal.lifecycle.PhaseId;
import org.eclipse.rap.rwt.internal.lifecycle.PhaseListener;
import org.eclipse.rap.rwt.internal.lifecycle.RWTLifeCycle;
import org.eclipse.rap.rwt.internal.lifecycle.UITestUtil;
import org.eclipse.rap.rwt.internal.lifecycle.WidgetAdapter;
import org.eclipse.rap.rwt.internal.lifecycle.WidgetLifeCycleAdapter;
import org.eclipse.rap.rwt.internal.lifecycle.WidgetUtil;
import org.eclipse.rap.rwt.internal.protocol.ClientMessageConst;
import org.eclipse.rap.rwt.internal.protocol.Operation.DestroyOperation;
import org.eclipse.rap.rwt.internal.protocol.Operation.SetOperation;
import org.eclipse.rap.rwt.internal.protocol.ProtocolMessageWriter;
import org.eclipse.rap.rwt.internal.remote.DeferredRemoteObject;
import org.eclipse.rap.rwt.internal.remote.RemoteObjectImpl;
import org.eclipse.rap.rwt.internal.remote.RemoteObjectRegistry;
import org.eclipse.rap.rwt.internal.serverpush.ServerPushManager;
import org.eclipse.rap.rwt.remote.OperationHandler;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.TestMessage;
import org.eclipse.rap.rwt.testfixture.TestRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.widgets.IDisplayAdapter;
import org.eclipse.swt.internal.widgets.WidgetAdapterImpl;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;


@SuppressWarnings( "deprecation" )
public class DisplayLCA_Test {

  private Display display;
  private String displayId;
  private DisplayLCA displayLCA;

  @Before
  public void setUp() {
    Fixture.setUp();
    display = new Display();
    displayId = DisplayUtil.getId( display );
    displayLCA = new DisplayLCA();
    Fixture.fakeNewRequest();
  }

  @After
  public void tearDown() {
    Fixture.tearDown();
    setEnableUiTests( false );
  }

  @Test
  public void testPreserveValues() {
    Shell shell = new Shell( display );
    new Button( shell, SWT.PUSH );
    Fixture.markInitialized( display );
    shell.setFocus();
    shell.open();

    Fixture.preserveWidgets();

    WidgetAdapter adapter = getAdapter( display );
    assertEquals( shell, adapter.getPreserved( DisplayLCA.PROP_FOCUS_CONTROL ) );
    Object exitConfirmation = adapter.getPreserved( DisplayLCA.PROP_EXIT_CONFIRMATION );
    assertNull( exitConfirmation );
  }

  @Test
  public void testRender() throws IOException {
    AbstractWidgetLCA lca = mock( AbstractWidgetLCA.class );
    Shell shell1 = new CustomLCAShell( display, lca );
    Widget button1 = new CustomLCAWidget( shell1, lca );
    Shell shell2 = new CustomLCAShell( display, lca );
    Widget button2 = new CustomLCAWidget( shell2, lca );

    displayLCA.render( display );

    InOrder inOrder = inOrder( lca );
    inOrder.verify( lca ).render( shell1 );
    inOrder.verify( lca ).render( button1 );
    inOrder.verify( lca ).render( shell2 );
    inOrder.verify( lca ).render( button2 );
  }

  @Test
  public void testRenderWithIOException() {
    Composite shell = new Shell( display , SWT.NONE );
    new CustomLCAWidget( shell, new TestWidgetLCA() {
      @Override
      public void renderChanges( Widget widget ) throws IOException {
        throw new IOException();
      }
    } );

    try {
      displayLCA.render( display );
      fail( "IOException of the renderer adapter in case of composite should be rethrown." );
    } catch( IOException expected ) {
    }
  }

  @Test
  public void testReadData() {
    AbstractWidgetLCA lca = mock( AbstractWidgetLCA.class );
    Composite shell = new CustomLCAShell( display, lca );
    Widget button = new CustomLCAWidget( shell, lca );
    Widget text = new CustomLCAWidget( shell, lca );

    displayLCA.readData( display );

    InOrder inOrder = inOrder( lca );
    inOrder.verify( lca ).readData( shell );
    inOrder.verify( lca ).readData( button );
    inOrder.verify( lca ).readData( text );
    verifyNoMoreInteractions( lca );
  }

  @Test
  public void testReadDisplayBounds() {
    Fixture.fakeSetProperty( getId( display ), "bounds", createJsonArray( 0, 0, 30, 70 ) );

    displayLCA.readData( display );

    assertEquals( new Rectangle( 0, 0, 30, 70 ), display.getBounds() );
  }

  @Test
  public void testReadCursorLocation() {
    Fixture.fakeSetProperty( getId( display ), "cursorLocation", createJsonArray( 1, 2 ) );

    displayLCA.readData( display );

    assertEquals( new Point( 1, 2 ), display.getCursorLocation() );
  }

  @Test
  public void testRenderInitialResizeListener() throws IOException {
    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findListenOperation( getId( display ), "Resize" ) );
  }

  @Test
  public void testRenderResizeListener() throws IOException {
    Listener listener = mock( Listener.class );
    display.addListener( SWT.Resize, listener  );

    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonValue.TRUE, message.findListenProperty( getId( display ), "Resize" ) );
  }

  @Test
  public void testRenderResizeListenerUnchanged() throws IOException {
    Listener listener = mock( Listener.class );
    display.addListener( SWT.Resize, listener  );

    displayLCA.preserveValues( display );
    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findListenOperation( getId( display ), "Resize" ) );
  }

  @Test
  public void testRenderWithChangedAndDisposedWidget() throws IOException {
    Shell shell = new Shell( display, SWT.NONE );
    AbstractWidgetLCA lca = mock( AbstractWidgetLCA.class );
    Composite composite = new CustomLCAWidget( shell, lca );
    Fixture.markInitialized( composite );
    Fixture.preserveWidgets();
    composite.setBounds( 1, 2, 3, 4 );
    composite.dispose();

    displayLCA.render( display );

    verify( lca ).renderDispose( composite );
    verifyNoMoreInteractions( lca );
  }

  @Test
  public void testIsInitializedState() throws IOException {
    final Boolean[] compositeInitState = new Boolean[] { null };
    Shell shell = new Shell( display, SWT.NONE );
    final Composite composite = new Shell( shell, SWT.NONE );
    Control control = new Button( composite, SWT.PUSH );
    WidgetAdapterImpl controlAdapter = ( WidgetAdapterImpl )getAdapter( control );
    controlAdapter.addRenderRunnable( new Runnable() {
      public void run() {
        boolean initState = getAdapter( composite ).isInitialized();
        compositeInitState[ 0 ] = Boolean.valueOf( initState );
      }
    } );
    // Ensure that the isInitialized state is to to true *right* after a widget
    // was rendered; as opposed to being set to true after the whole widget
    // tree was rendered
    // check precondition
    assertFalse( getAdapter( composite ).isInitialized() );

    displayLCA.render( display );

    assertEquals( Boolean.TRUE, compositeInitState[ 0 ] );
  }

  @Test
  public void testRenderInitiallyDisposed() {
    ApplicationContextImpl applicationContext = getApplicationContext();
    applicationContext.getEntryPointManager().register( TestRequest.DEFAULT_SERVLET_PATH,
                                                        TestRenderInitiallyDisposedEntryPoint.class,
                                                        null );
    RWTLifeCycle lifeCycle
      = ( RWTLifeCycle )getApplicationContext().getLifeCycleFactory().getLifeCycle();
    LifeCycleUtil.setSessionDisplay( null );
    // ensure that life cycle execution succeeds with disposed display
    try {
      lifeCycle.execute();
    } catch( Throwable e ) {
      fail( "Life cycle execution must succeed even with a disposed display" );
    }
  }

  @Test
  public void testRenderDisposed() throws Exception {
    ApplicationContextImpl applicationContext = getApplicationContext();
    applicationContext.getEntryPointManager().register( TestRequest.DEFAULT_SERVLET_PATH,
                                                        TestRenderDisposedEntryPoint.class,
                                                        null );
    RWTLifeCycle lifeCycle = ( RWTLifeCycle )applicationContext.getLifeCycleFactory().getLifeCycle();
    LifeCycleUtil.setSessionDisplay( null );
    lifeCycle.execute();
    Fixture.fakeNewRequest();
    lifeCycle.execute();
    Fixture.fakeNewRequest();
    final Shell[] shell = new Shell[ 1 ];
    lifeCycle.addPhaseListener( new PhaseListener() {
      private static final long serialVersionUID = 1L;
      public PhaseId getPhaseId() {
        return PhaseId.PROCESS_ACTION;
      }
      public void beforePhase( PhaseEvent event ) {
        shell[ 0 ] = Display.getCurrent().getShells()[ 0 ];
        shell[ 0 ].dispose();
      }
      public void afterPhase( PhaseEvent event ) {
      }
    } );

    lifeCycle.execute();

    TestMessage message = Fixture.getProtocolMessage();
    assertTrue( message.getOperation( 0 ) instanceof DestroyOperation );
    String shellId = WidgetUtil.getId( shell[ 0 ] );
    assertEquals( shellId, message.getOperation( 0 ).getTarget() );
  }

  @Test
  public void testRenderRunnablesAreExecutedAndCleared_onWidget() throws IOException {
    Widget widget = new Shell( display );
    WidgetAdapterImpl widgetAdapter = ( WidgetAdapterImpl )getAdapter( widget );
    Runnable renderRunnable = mock( Runnable.class );
    widgetAdapter.addRenderRunnable( renderRunnable );

    displayLCA.render( display );

    verify( renderRunnable ).run();
    assertEquals( 0, widgetAdapter.getRenderRunnables().length );
  }

  @Test
  public void testRenderRunnablesAreExecutedAndCleared_onDisplay() throws IOException {
    WidgetAdapterImpl widgetAdapter = ( WidgetAdapterImpl )getAdapter( display );
    Runnable renderRunnable = mock( Runnable.class );
    widgetAdapter.addRenderRunnable( renderRunnable );

    displayLCA.render( display );

    verify( renderRunnable ).run();
    assertEquals( 0, widgetAdapter.getRenderRunnables().length );
  }

  @Test
  public void testServerPushRendered() throws IOException {
    ServerPushManager.getInstance().activateServerPushFor( new Object() );

    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertNotNull( message.findSetProperty( "rwt.client.ServerPush", "active" ) );
  }

  @Test
  public void testRenderBeep() throws IOException {
    display.beep();
    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertNotNull( message.findCallOperation( displayId, "beep" ) );
    assertFalse( display.getAdapter( IDisplayAdapter.class ).isBeepCalled() );
  }

  @Test
  public void testRenderEnableUiTests() throws IOException {
    setEnableUiTests( true );

    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    // must be the first operation, before any widgets are rendered
    SetOperation firstOperation = (SetOperation)message.getOperation( 0 );
    assertEquals( DisplayUtil.getId( display ), firstOperation.getTarget() );
    assertEquals( JsonValue.TRUE, firstOperation.getProperties().get( "enableUiTests" ) );
  }

  @Test
  public void testRenderEnableUiTests_WhenAlreadyInitialized() throws IOException {
    Fixture.markInitialized( display );
    setEnableUiTests( true );

    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( displayId, "enableUiTests" ) );
  }

  @Test
  public void testRendersExitConfirmation() throws IOException {
    RWT.getClient().getService( ExitConfirmation.class ).setMessage( "test" );

    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( "test", message.findSetProperty( displayId, "exitConfirmation" ).asString() );
  }

  @Test
  public void testPreservesExitConfirmation() throws IOException {
    RWT.getClient().getService( ExitConfirmation.class ).setMessage( "test" );

    displayLCA.preserveValues( display );
    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( displayId, "exitConfirmation" ) );
  }

  @Test
  public void testRendersExitConfirmationReset() throws IOException {
    RWT.getClient().getService( ExitConfirmation.class ).setMessage( "test" );
    displayLCA.preserveValues( display );

    RWT.getClient().getService( ExitConfirmation.class ).setMessage( null );
    displayLCA.render( display );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonObject.NULL, message.findSetProperty( displayId, "exitConfirmation" ) );
  }

  @Test
  public void testRendersRemoteObjects() throws IOException {
    DeferredRemoteObject remoteObject = mock( DeferredRemoteObject.class );
    when( remoteObject.getId() ).thenReturn( "id" );
    RemoteObjectRegistry.getInstance().register( remoteObject );

    displayLCA.render( display );

    verify( remoteObject ).render( any( ProtocolMessageWriter.class ) );
  }

  @Test
  public void testReadDataDelegatesToRemoteObjects() {
    OperationHandler handler = mock( OperationHandler.class );
    RemoteObjectImpl remoteObject = mock( DeferredRemoteObject.class );
    when( remoteObject.getId() ).thenReturn( "id" );
    when( remoteObject.getHandler() ).thenReturn( handler );
    RemoteObjectRegistry.getInstance().register( remoteObject );
    JsonObject properties = new JsonObject().add( "foo", "bar" );
    Fixture.fakeCallOperation( "id", "method", properties );

    displayLCA.readData( display );

    verify( handler ).handleCall( eq( "method" ), eq( new JsonObject().add( "foo", "bar" ) ) );
  }

  @Test
  public void testFocusControl_doesNotRenderBack() {
    Fixture.markInitialized( display );
    Shell shell = new Shell( display, SWT.NONE );
    new Button( shell, SWT.PUSH );
    Control control = new Button( shell, SWT.PUSH );

    Fixture.fakeSetProperty( getId( display ), "focusControl", getId( control ) );
    Fixture.executeLifeCycleFromServerThread();

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( getId( display ), "focusControl" ) );
  }

  /* Test case to simulate the scenario reported in this bug:
   * 196911: Invalid value Text#getText with opened shell
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=196911
   */
  @Test
  public void testFocusControl_renderBackIsEnforced() {
    Fixture.markInitialized( display );
    final Shell shell = new Shell( display, SWT.NONE );
    final Button button = new Button( shell, SWT.PUSH );
    final Shell[] childShell = { null };
    button.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        childShell[ 0 ] = new Shell( shell, SWT.NONE );
        childShell[ 0 ].setBounds( 0, 0, 100, 100 );
        childShell[ 0 ].open();
        button.setFocus();
      }
    } );
    shell.open();

    // Simulate initial request that constructs UI
    Fixture.fakeNewRequest();
    Fixture.executeLifeCycleFromServerThread();

    // Simulate request that is sent when button was pressed
    Fixture.fakeNewRequest();
    Fixture.fakeNotifyOperation( getId( button ), ClientMessageConst.EVENT_SELECTION, null );
    Fixture.fakeSetProperty( getId( display ), "focusControl", getId( button ) );
    Fixture.executeLifeCycleFromServerThread();

    // ensure that widgetSelected was called
    assertNotNull( childShell[ 0 ] );
    TestMessage message = Fixture.getProtocolMessage();
    String focusControlId = message.findSetProperty( getId( display ), "focusControl" ).asString();
    assertEquals( getId( button ), focusControlId );
  }

  private static void setEnableUiTests( boolean value ) {
    Field field;
    try {
      field = UITestUtil.class.getDeclaredField( "enabled" );
      field.setAccessible( true );
      field.setBoolean( null, value );
    } catch( Exception e ) {
      throw new RuntimeException( "Failed to set enabled field", e );
    }
  }

  private static class TestWidgetLCA extends AbstractWidgetLCA {
    @Override
    public void readData( Widget widget ) {
    }
    @Override
    public void preserveValues( Widget widget ) {
    }
    @Override
    public void renderInitialization( Widget widget ) throws IOException {
    }
    @Override
    public void renderChanges( Widget widget ) throws IOException {
    }
    @Override
    public void renderDispose( Widget widget ) throws IOException {
    }
  }

  private static class CustomLCAWidget extends Composite {
    private static final long serialVersionUID = 1L;

    private final AbstractWidgetLCA widgetLCA;

    CustomLCAWidget( Composite parent, AbstractWidgetLCA widgetLCA ) {
      super( parent, 0 );
      this.widgetLCA = widgetLCA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter( Class<T> adapter ) {
      Object result;
      if( adapter == WidgetLifeCycleAdapter.class ) {
        result = widgetLCA;
      } else {
        result = super.getAdapter( adapter );
      }
      return ( T )result;
    }
  }

  private static class CustomLCAShell extends Shell {
    private static final long serialVersionUID = 1L;

    private final AbstractWidgetLCA widgetLCA;

    CustomLCAShell( Display display, AbstractWidgetLCA widgetLCA ) {
      super( display );
      this.widgetLCA = widgetLCA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter( Class<T> adapter ) {
      Object result;
      if( adapter == WidgetLifeCycleAdapter.class ) {
        result = widgetLCA;
      } else {
        result = super.getAdapter( adapter );
      }
      return ( T )result;
    }
  }

  public static final class TestRenderInitiallyDisposedEntryPoint implements EntryPoint {
    public int createUI() {
      Display display = new Display();
      display.dispose();
      return 0;
    }
  }

  public static final class TestRenderDisposedEntryPoint implements EntryPoint {
    public int createUI() {
      Display display = new Display();
      Shell shell = new Shell( display );
      while( !shell.isDisposed() ) {
        if( !display.readAndDispatch() ) {
          display.sleep();
        }
      }
      display.dispose();
      return 0;
    }
  }

}
