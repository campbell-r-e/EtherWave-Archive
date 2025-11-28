import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, Message, StompSubscription } from '@stomp/stompjs';
import { QSO } from '../models/qso.model';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private client: Client | null = null;
  private connected = false;

  private qsoSubject = new Subject<QSO>();
  private telemetrySubject = new Subject<any>();

  constructor() {
    this.connect();
  }

  private connect(): void {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        console.log('STOMP:', str);
      },
      onConnect: () => {
        console.log('WebSocket connected');
        this.connected = true;
        this.subscribeToTopics();
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        this.connected = false;
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      }
    });

    this.client.activate();
  }

  private subscribeToTopics(): void {
    if (!this.client) return;

    // Subscribe to new QSO notifications
    this.client.subscribe('/topic/qsos', (message: Message) => {
      const qso: QSO = JSON.parse(message.body);
      this.qsoSubject.next(qso);
    });

    // Subscribe to telemetry updates (all stations)
    this.client.subscribe('/topic/telemetry/*', (message: Message) => {
      const telemetry = JSON.parse(message.body);
      this.telemetrySubject.next(telemetry);
    });
  }

  // Observable for new QSOs
  getQSOUpdates(): Observable<QSO> {
    return this.qsoSubject.asObservable();
  }

  // Observable for telemetry updates
  getTelemetryUpdates(): Observable<any> {
    return this.telemetrySubject.asObservable();
  }

  // Subscribe to specific station telemetry
  subscribeToStationTelemetry(stationId: number, callback: (data: any) => void): StompSubscription | null {
    if (!this.client || !this.connected) return null;

    return this.client.subscribe(`/topic/telemetry/${stationId}`, (message: Message) => {
      const telemetry = JSON.parse(message.body);
      callback(telemetry);
    });
  }

  isConnected(): boolean {
    return this.connected;
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
    }
  }
}
