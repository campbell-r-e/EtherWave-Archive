import { TestBed } from '@angular/core/testing';
import { WebSocketService } from './websocket.service';
import { Subject } from 'rxjs';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let mockStompClient: any;

  beforeEach(() => {
    // Mock SockJS and Stomp
    (window as any).SockJS = jasmine.createSpy('SockJS').and.returnValue({
      onopen: null,
      onclose: null,
      onerror: null
    });

    mockStompClient = {
      connect: jasmine.createSpy('connect'),
      disconnect: jasmine.createSpy('disconnect'),
      subscribe: jasmine.createSpy('subscribe'),
      send: jasmine.createSpy('send'),
      connected: false
    };

    (window as any).Stomp = {
      over: jasmine.createSpy('over').and.returnValue(mockStompClient)
    };

    TestBed.configureTestingModule({});
    service = TestBed.inject(WebSocketService);
  });

  afterEach(() => {
    service.disconnect();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ==================== CONNECTION TESTS ====================

  it('should connect to WebSocket server', () => {
    service.connect();

    expect(mockStompClient.connect).toHaveBeenCalled();
  });

  it('should use correct WebSocket URL', () => {
    service.connect();

    expect((window as any).SockJS).toHaveBeenCalledWith(jasmine.stringContaining('/ws'));
  });

  it('should handle successful connection', (done) => {
    mockStompClient.connect.and.callFake((headers: any, successCallback: Function) => {
      mockStompClient.connected = true;
      successCallback();
    });

    service.connectionStatus$.subscribe(status => {
      if (status === 'connected') {
        expect(service.isConnected()).toBeTruthy();
        done();
      }
    });

    service.connect();
  });

  it('should handle connection failure', (done) => {
    mockStompClient.connect.and.callFake((headers: any, successCallback: Function, errorCallback: Function) => {
      errorCallback(new Error('Connection failed'));
    });

    service.connectionStatus$.subscribe(status => {
      if (status === 'error') {
        expect(service.isConnected()).toBeFalsy();
        done();
      }
    });

    service.connect();
  });

  it('should not connect if already connected', () => {
    mockStompClient.connected = true;

    service.connect();

    expect(mockStompClient.connect).not.toHaveBeenCalled();
  });

  it('should emit connection status', (done) => {
    mockStompClient.connect.and.callFake((headers: any, successCallback: Function) => {
      mockStompClient.connected = true;
      successCallback();
    });

    service.connectionStatus$.subscribe(status => {
      if (status === 'connected') {
        expect(status).toBe('connected');
        done();
      }
    });

    service.connect();
  });

  // ==================== DISCONNECTION TESTS ====================

  it('should disconnect from WebSocket server', () => {
    mockStompClient.connected = true;

    service.disconnect();

    expect(mockStompClient.disconnect).toHaveBeenCalled();
  });

  it('should update connection status on disconnect', (done) => {
    mockStompClient.connected = true;

    service.connectionStatus$.subscribe(status => {
      if (status === 'disconnected') {
        expect(service.isConnected()).toBeFalsy();
        done();
      }
    });

    service.disconnect();
  });

  it('should not disconnect if already disconnected', () => {
    mockStompClient.connected = false;

    service.disconnect();

    expect(mockStompClient.disconnect).not.toHaveBeenCalled();
  });

  it('should clear all subscriptions on disconnect', () => {
    mockStompClient.connected = true;
    const sub1 = { unsubscribe: jasmine.createSpy('unsubscribe') };
    const sub2 = { unsubscribe: jasmine.createSpy('unsubscribe') };

    service['subscriptions'] = [sub1, sub2];

    service.disconnect();

    expect(sub1.unsubscribe).toHaveBeenCalled();
    expect(sub2.unsubscribe).toHaveBeenCalled();
  });

  // ==================== SUBSCRIPTION TESTS ====================

  it('should subscribe to topic', (done) => {
    mockStompClient.connected = true;
    mockStompClient.subscribe.and.returnValue({ unsubscribe: () => {} });

    const subscription = service.subscribe('/topic/qsos');

    subscription.subscribe(message => {
      expect(message).toBeTruthy();
      done();
    });

    expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/qsos', jasmine.any(Function));
  });

  it('should handle subscription messages', (done) => {
    mockStompClient.connected = true;
    let messageHandler: Function;

    mockStompClient.subscribe.and.callFake((topic: string, handler: Function) => {
      messageHandler = handler;
      return { unsubscribe: () => {} };
    });

    service.subscribe('/topic/qsos').subscribe(message => {
      expect(message).toEqual({ id: 1, callsign: 'W1AW' });
      done();
    });

    // Simulate incoming message
    messageHandler({ body: JSON.stringify({ id: 1, callsign: 'W1AW' }) });
  });

  it('should parse JSON message body', (done) => {
    mockStompClient.connected = true;
    let messageHandler: Function;

    mockStompClient.subscribe.and.callFake((topic: string, handler: Function) => {
      messageHandler = handler;
      return { unsubscribe: () => {} };
    });

    service.subscribe('/topic/qsos').subscribe(message => {
      expect(typeof message).toBe('object');
      expect(message.callsign).toBe('W1AW');
      done();
    });

    messageHandler({ body: JSON.stringify({ callsign: 'W1AW' }) });
  });

  it('should handle invalid JSON in message', () => {
    mockStompClient.connected = true;
    let messageHandler: Function;

    mockStompClient.subscribe.and.callFake((topic: string, handler: Function) => {
      messageHandler = handler;
      return { unsubscribe: () => {} };
    });

    service.subscribe('/topic/qsos').subscribe(
      () => {},
      error => {
        expect(error).toBeTruthy();
      }
    );

    messageHandler({ body: 'invalid-json' });
  });

  it('should support multiple subscriptions', () => {
    mockStompClient.connected = true;
    mockStompClient.subscribe.and.returnValue({ unsubscribe: () => {} });

    service.subscribe('/topic/qsos');
    service.subscribe('/topic/logs');
    service.subscribe('/topic/rig-status');

    expect(mockStompClient.subscribe).toHaveBeenCalledTimes(3);
  });

  it('should unsubscribe from topic', () => {
    mockStompClient.connected = true;
    const mockSubscription = { unsubscribe: jasmine.createSpy('unsubscribe') };
    mockStompClient.subscribe.and.returnValue(mockSubscription);

    const subscription = service.subscribe('/topic/qsos');
    subscription.unsubscribe();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
  });

  // ==================== SEND MESSAGE TESTS ====================

  it('should send message to destination', () => {
    mockStompClient.connected = true;
    const message = { id: 1, callsign: 'W1AW' };

    service.send('/app/qso', message);

    expect(mockStompClient.send).toHaveBeenCalledWith(
      '/app/qso',
      {},
      JSON.stringify(message)
    );
  });

  it('should not send if disconnected', () => {
    mockStompClient.connected = false;
    const message = { id: 1, callsign: 'W1AW' };

    service.send('/app/qso', message);

    expect(mockStompClient.send).not.toHaveBeenCalled();
  });

  it('should serialize message as JSON', () => {
    mockStompClient.connected = true;
    const message = { id: 1, callsign: 'W1AW', frequencyKhz: 14250 };

    service.send('/app/qso', message);

    expect(mockStompClient.send).toHaveBeenCalledWith(
      '/app/qso',
      {},
      JSON.stringify(message)
    );
  });

  it('should include custom headers', () => {
    mockStompClient.connected = true;
    const message = { id: 1 };
    const headers = { 'X-Custom-Header': 'value' };

    service.send('/app/qso', message, headers);

    expect(mockStompClient.send).toHaveBeenCalledWith(
      '/app/qso',
      headers,
      JSON.stringify(message)
    );
  });

  // ==================== AUTO-RECONNECT TESTS ====================

  it('should attempt reconnection on failure', () => {
    jasmine.clock().install();

    mockStompClient.connect.and.callFake((headers: any, successCallback: Function, errorCallback: Function) => {
      errorCallback(new Error('Connection failed'));
    });

    service.connect();
    jasmine.clock().tick(5000); // Wait for reconnect interval

    expect(mockStompClient.connect).toHaveBeenCalledTimes(2);

    jasmine.clock().uninstall();
  });

  it('should stop reconnecting after max attempts', () => {
    jasmine.clock().install();

    mockStompClient.connect.and.callFake((headers: any, successCallback: Function, errorCallback: Function) => {
      errorCallback(new Error('Connection failed'));
    });

    service.connect();

    for (let i = 0; i < 10; i++) {
      jasmine.clock().tick(5000);
    }

    expect(service['reconnectAttempts']).toBeLessThanOrEqual(service['maxReconnectAttempts']);

    jasmine.clock().uninstall();
  });

  it('should reset reconnect attempts on successful connection', () => {
    mockStompClient.connect.and.callFake((headers: any, successCallback: Function) => {
      mockStompClient.connected = true;
      successCallback();
    });

    service['reconnectAttempts'] = 3;
    service.connect();

    expect(service['reconnectAttempts']).toBe(0);
  });

  // ==================== CONNECTION STATUS TESTS ====================

  it('should track connection state', () => {
    expect(service.isConnected()).toBeFalsy();

    mockStompClient.connected = true;
    expect(service.isConnected()).toBeTruthy();
  });

  it('should emit connecting status', (done) => {
    service.connectionStatus$.subscribe(status => {
      if (status === 'connecting') {
        expect(status).toBe('connecting');
        done();
      }
    });

    service.connect();
  });

  it('should emit reconnecting status', (done) => {
    jasmine.clock().install();

    mockStompClient.connect.and.callFake((headers: any, successCallback: Function, errorCallback: Function) => {
      errorCallback(new Error('Connection failed'));
    });

    service.connectionStatus$.subscribe(status => {
      if (status === 'reconnecting') {
        expect(status).toBe('reconnecting');
        jasmine.clock().uninstall();
        done();
      }
    });

    service.connect();
    jasmine.clock().tick(5000);
  });

  // ==================== HEARTBEAT TESTS ====================

  it('should send heartbeat messages', () => {
    jasmine.clock().install();

    mockStompClient.connected = true;

    service.startHeartbeat();
    jasmine.clock().tick(30000); // Default heartbeat interval

    expect(mockStompClient.send).toHaveBeenCalledWith('/app/heartbeat', {}, jasmine.any(String));

    jasmine.clock().uninstall();
  });

  it('should stop heartbeat on disconnect', () => {
    jasmine.clock().install();

    mockStompClient.connected = true;
    service.startHeartbeat();

    service.disconnect();
    jasmine.clock().tick(30000);

    expect(mockStompClient.send).not.toHaveBeenCalled();

    jasmine.clock().uninstall();
  });

  // ==================== ERROR HANDLING TESTS ====================

  it('should handle subscription error', (done) => {
    mockStompClient.connected = true;
    mockStompClient.subscribe.and.throwError('Subscription failed');

    service.subscribe('/topic/qsos').subscribe(
      () => fail('should have failed'),
      error => {
        expect(error).toBeTruthy();
        done();
      }
    );
  });

  it('should handle send error', () => {
    mockStompClient.connected = true;
    mockStompClient.send.and.throwError('Send failed');

    expect(() => service.send('/app/qso', {})).not.toThrow();
  });

  it('should handle connection timeout', (done) => {
    jasmine.clock().install();

    mockStompClient.connect.and.callFake(() => {
      // Never call success or error callback
    });

    service.connectionStatus$.subscribe(status => {
      if (status === 'timeout') {
        expect(status).toBe('timeout');
        jasmine.clock().uninstall();
        done();
      }
    });

    service.connect();
    jasmine.clock().tick(10000); // Connection timeout
  });

  // ==================== MESSAGE QUEUE TESTS ====================

  it('should queue messages when disconnected', () => {
    mockStompClient.connected = false;

    service.send('/app/qso', { id: 1 });
    service.send('/app/qso', { id: 2 });

    expect(service['messageQueue'].length).toBe(2);
  });

  it('should flush queue on connection', () => {
    mockStompClient.connected = false;

    service.send('/app/qso', { id: 1 });
    service.send('/app/qso', { id: 2 });

    mockStompClient.connected = true;
    mockStompClient.connect.and.callFake((headers: any, successCallback: Function) => {
      mockStompClient.connected = true;
      successCallback();
    });

    service.connect();

    expect(mockStompClient.send).toHaveBeenCalledTimes(2);
  });

  it('should clear queue after flushing', () => {
    mockStompClient.connected = false;

    service.send('/app/qso', { id: 1 });

    mockStompClient.connect.and.callFake((headers: any, successCallback: Function) => {
      mockStompClient.connected = true;
      successCallback();
    });

    service.connect();

    expect(service['messageQueue'].length).toBe(0);
  });

  // ==================== TOPIC PATTERNS TESTS ====================

  it('should subscribe to QSO updates', (done) => {
    mockStompClient.connected = true;
    mockStompClient.subscribe.and.returnValue({ unsubscribe: () => {} });

    service.subscribeToQSOUpdates().subscribe(() => {
      done();
    });

    expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/qsos', jasmine.any(Function));
  });

  it('should subscribe to log updates', (done) => {
    mockStompClient.connected = true;
    mockStompClient.subscribe.and.returnValue({ unsubscribe: () => {} });

    service.subscribeToLogUpdates().subscribe(() => {
      done();
    });

    expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/logs', jasmine.any(Function));
  });

  it('should subscribe to rig status updates', (done) => {
    mockStompClient.connected = true;
    mockStompClient.subscribe.and.returnValue({ unsubscribe: () => {} });

    service.subscribeToRigStatus().subscribe(() => {
      done();
    });

    expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/rig-status', jasmine.any(Function));
  });

  it('should subscribe to invitation updates', (done) => {
    mockStompClient.connected = true;
    mockStompClient.subscribe.and.returnValue({ unsubscribe: () => {} });

    service.subscribeToInvitations().subscribe(() => {
      done();
    });

    expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/invitations', jasmine.any(Function));
  });

  // ==================== CLEANUP TESTS ====================

  it('should cleanup on destroy', () => {
    mockStompClient.connected = true;
    spyOn(service, 'disconnect');

    service.ngOnDestroy();

    expect(service.disconnect).toHaveBeenCalled();
  });

  it('should stop heartbeat on destroy', () => {
    jasmine.clock().install();

    service.startHeartbeat();
    service.ngOnDestroy();

    jasmine.clock().tick(30000);

    expect(mockStompClient.send).not.toHaveBeenCalled();

    jasmine.clock().uninstall();
  });

  // ==================== CONFIGURATION TESTS ====================

  it('should use custom WebSocket URL', () => {
    const customUrl = 'wss://custom.example.com/ws';

    service.setWebSocketUrl(customUrl);
    service.connect();

    expect((window as any).SockJS).toHaveBeenCalledWith(customUrl);
  });

  it('should set reconnect interval', () => {
    service.setReconnectInterval(10000);

    expect(service['reconnectInterval']).toBe(10000);
  });

  it('should set max reconnect attempts', () => {
    service.setMaxReconnectAttempts(5);

    expect(service['maxReconnectAttempts']).toBe(5);
  });

  // ==================== STATISTICS TESTS ====================

  it('should track message count', () => {
    mockStompClient.connected = true;

    service.send('/app/qso', { id: 1 });
    service.send('/app/qso', { id: 2 });

    expect(service.getMessagesSent()).toBe(2);
  });

  it('should track connection uptime', () => {
    jasmine.clock().install();

    mockStompClient.connect.and.callFake((headers: any, successCallback: Function) => {
      mockStompClient.connected = true;
      successCallback();
    });

    service.connect();
    jasmine.clock().tick(60000); // 1 minute

    expect(service.getConnectionUptime()).toBeGreaterThan(0);

    jasmine.clock().uninstall();
  });

  it('should reset statistics on disconnect', () => {
    mockStompClient.connected = true;
    service.send('/app/qso', { id: 1 });

    service.disconnect();

    expect(service.getMessagesSent()).toBe(0);
  });
});
