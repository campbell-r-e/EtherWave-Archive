import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PropagationService, PropagationConditions, BandCondition } from '../../services/propagation/propagation.service';

@Component({
  selector: 'app-propagation-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './propagation-panel.component.html',
  styleUrls: ['./propagation-panel.component.css']
})
export class PropagationPanelComponent implements OnInit {
  conditions: PropagationConditions | null = null;
  loading = false;
  error: string | null = null;
  isExpanded = true;

  // Display order for bands
  readonly BAND_ORDER = ['160m', '80m', '40m', '20m', '17m', '15m', '12m', '10m', '6m'];

  constructor(private propagationService: PropagationService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    this.propagationService.getConditions().subscribe({
      next: (conditions) => {
        this.conditions = conditions;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load propagation data.';
        this.loading = false;
      }
    });
  }

  togglePanel(): void {
    this.isExpanded = !this.isExpanded;
  }

  get bandList() {
    if (!this.conditions) return [];
    return this.BAND_ORDER
      .map(b => this.conditions!.bands[b])
      .filter(Boolean);
  }

  conditionClass(condition: BandCondition): string {
    return {
      EXCELLENT: 'cond-excellent',
      GOOD: 'cond-good',
      FAIR: 'cond-fair',
      POOR: 'cond-poor'
    }[condition] || '';
  }

  conditionLabel(condition: BandCondition): string {
    return {
      EXCELLENT: 'Excellent',
      GOOD: 'Good',
      FAIR: 'Fair',
      POOR: 'Poor'
    }[condition] || condition;
  }

  /** K-index severity label */
  kLabel(k: number): string {
    if (k <= 1) return 'Quiet';
    if (k <= 2) return 'Unsettled';
    if (k <= 3) return 'Active';
    if (k <= 4) return 'Minor Storm';
    if (k <= 5) return 'Moderate Storm';
    return 'Major Storm';
  }

  /** SFI descriptor */
  sfiLabel(sfi: number): string {
    if (sfi >= 200) return 'Very High';
    if (sfi >= 150) return 'High';
    if (sfi >= 100) return 'Moderate';
    if (sfi >= 80) return 'Low';
    return 'Very Low';
  }
}
