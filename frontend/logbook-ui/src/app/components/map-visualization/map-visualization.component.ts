import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { WebSocketService } from '../../services/websocket.service';
import { Subscription } from 'rxjs';

interface StateStats {
  state: string;
  count: number;
  stateName: string;
}

@Component({
    selector: 'app-map-visualization',
    imports: [CommonModule],
    templateUrl: './map-visualization.component.html',
    styleUrls: ['./map-visualization.component.css']
})
export class MapVisualizationComponent implements OnInit, OnDestroy {
  stateStats: Map<string, StateStats> = new Map();
  maxCount = 0;
  private wsSubscription: Subscription | null = null;

  // US States organized by region for grid layout
  usStates = {
    northeast: ['ME', 'NH', 'VT', 'MA', 'RI', 'CT', 'NY', 'NJ', 'PA'],
    southeast: ['DE', 'MD', 'VA', 'WV', 'NC', 'SC', 'GA', 'FL', 'KY', 'TN', 'AL', 'MS', 'LA', 'AR'],
    midwest: ['OH', 'IN', 'IL', 'MI', 'WI', 'MN', 'IA', 'MO', 'ND', 'SD', 'NE', 'KS'],
    southwest: ['OK', 'TX', 'NM', 'AZ'],
    west: ['MT', 'WY', 'CO', 'ID', 'UT', 'NV', 'CA', 'OR', 'WA', 'AK', 'HI']
  };

  canadianProvinces = ['AB', 'BC', 'MB', 'NB', 'NL', 'NS', 'NT', 'NU', 'ON', 'PE', 'QC', 'SK', 'YT'];

  stateNames: { [key: string]: string } = {
    'AL': 'Alabama', 'AK': 'Alaska', 'AZ': 'Arizona', 'AR': 'Arkansas', 'CA': 'California',
    'CO': 'Colorado', 'CT': 'Connecticut', 'DE': 'Delaware', 'FL': 'Florida', 'GA': 'Georgia',
    'HI': 'Hawaii', 'ID': 'Idaho', 'IL': 'Illinois', 'IN': 'Indiana', 'IA': 'Iowa',
    'KS': 'Kansas', 'KY': 'Kentucky', 'LA': 'Louisiana', 'ME': 'Maine', 'MD': 'Maryland',
    'MA': 'Massachusetts', 'MI': 'Michigan', 'MN': 'Minnesota', 'MS': 'Mississippi', 'MO': 'Missouri',
    'MT': 'Montana', 'NE': 'Nebraska', 'NV': 'Nevada', 'NH': 'New Hampshire', 'NJ': 'New Jersey',
    'NM': 'New Mexico', 'NY': 'New York', 'NC': 'North Carolina', 'ND': 'North Dakota', 'OH': 'Ohio',
    'OK': 'Oklahoma', 'OR': 'Oregon', 'PA': 'Pennsylvania', 'RI': 'Rhode Island', 'SC': 'South Carolina',
    'SD': 'South Dakota', 'TN': 'Tennessee', 'TX': 'Texas', 'UT': 'Utah', 'VT': 'Vermont',
    'VA': 'Virginia', 'WA': 'Washington', 'WV': 'West Virginia', 'WI': 'Wisconsin', 'WY': 'Wyoming',
    'AB': 'Alberta', 'BC': 'British Columbia', 'MB': 'Manitoba', 'NB': 'New Brunswick',
    'NL': 'Newfoundland', 'NS': 'Nova Scotia', 'NT': 'Northwest Territories', 'NU': 'Nunavut',
    'ON': 'Ontario', 'PE': 'Prince Edward Island', 'QC': 'Quebec', 'SK': 'Saskatchewan', 'YT': 'Yukon'
  };

  constructor(
    private apiService: ApiService,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadStateStats();
    this.subscribeToUpdates();
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
  }

  loadStateStats(): void {
    this.apiService.getStateStatistics().subscribe({
      next: (stats) => {
        stats.forEach(stat => {
          this.stateStats.set(stat.state, {
            state: stat.state,
            count: stat.count,
            stateName: this.stateNames[stat.state] || stat.state
          });
          this.maxCount = Math.max(this.maxCount, stat.count);
        });
      },
      error: (err) => console.error('Error loading state stats:', err)
    });
  }

  subscribeToUpdates(): void {
    this.wsSubscription = this.wsService.getQSOUpdates().subscribe({
      next: (qso) => {
        if (qso.state) {
          const existing = this.stateStats.get(qso.state);
          if (existing) {
            existing.count++;
            this.maxCount = Math.max(this.maxCount, existing.count);
          } else {
            this.stateStats.set(qso.state, {
              state: qso.state,
              count: 1,
              stateName: this.stateNames[qso.state] || qso.state
            });
          }
        }
      }
    });
  }

  getStateCount(state: string): number {
    return this.stateStats.get(state)?.count || 0;
  }

  getStateColor(state: string): string {
    const count = this.getStateCount(state);
    if (count === 0) return '#f8f9fa';

    const intensity = Math.min(count / this.maxCount, 1);

    // Color gradient: light green → dark green
    if (intensity < 0.2) return '#d4edda';
    if (intensity < 0.4) return '#9fdf9f';
    if (intensity < 0.6) return '#6fbf73';
    if (intensity < 0.8) return '#4a9f4d';
    return '#2d7a2e';
  }

  getStateStyle(state: string): { [key: string]: string } {
    return {
      'background-color': this.getStateColor(state),
      'color': this.getStateCount(state) > this.maxCount * 0.5 ? '#ffffff' : '#000000'
    };
  }

  getTotalStatesWorked(): number {
    return Array.from(this.stateStats.values()).filter(s => s.count > 0).length;
  }

  getTotalQSOs(): number {
    return Array.from(this.stateStats.values()).reduce((sum, s) => sum + s.count, 0);
  }

  getRegionStates(region: keyof typeof this.usStates): string[] {
    return this.usStates[region];
  }

  getLegendColors(): { color: string; label: string }[] {
    return [
      { color: '#f8f9fa', label: 'No contacts' },
      { color: '#d4edda', label: '1-20%' },
      { color: '#9fdf9f', label: '21-40%' },
      { color: '#6fbf73', label: '41-60%' },
      { color: '#4a9f4d', label: '61-80%' },
      { color: '#2d7a2e', label: '81-100%' }
    ];
  }
}
