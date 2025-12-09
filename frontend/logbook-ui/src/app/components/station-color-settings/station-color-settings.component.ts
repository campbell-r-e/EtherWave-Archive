import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StationColorPreferencesService, StationColorConfig } from '../../services/station-colors/station-color-preferences.service';

interface StationColorItem {
  key: keyof StationColorConfig;
  label: string;
  description: string;
  icon: string;
}

/**
 * Station Color Settings Component
 *
 * Allows users to customize colors for stations 1-6 and GOTA.
 * Colors are used throughout the application for map markers,
 * badges, charts, and other visualizations.
 */
@Component({
  selector: 'app-station-color-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './station-color-settings.component.html',
  styleUrls: ['./station-color-settings.component.css']
})
export class StationColorSettingsComponent implements OnInit {
  colors: StationColorConfig;
  isCustomized: boolean = false;
  showResetConfirm: boolean = false;

  stationItems: StationColorItem[] = [
    {
      key: 'station1',
      label: 'Station 1',
      description: 'Primary station color',
      icon: '1'
    },
    {
      key: 'station2',
      label: 'Station 2',
      description: 'Secondary station color',
      icon: '2'
    },
    {
      key: 'station3',
      label: 'Station 3',
      description: 'Tertiary station color',
      icon: '3'
    },
    {
      key: 'station4',
      label: 'Station 4',
      description: 'Quaternary station color',
      icon: '4'
    },
    {
      key: 'station5',
      label: 'Station 5',
      description: 'Quinary station color',
      icon: '5'
    },
    {
      key: 'station6',
      label: 'Station 6',
      description: 'Senary station color',
      icon: '6'
    },
    {
      key: 'gota',
      label: 'GOTA Station',
      description: 'Get On The Air station color',
      icon: 'G'
    }
  ];

  constructor(
    private colorService: StationColorPreferencesService
  ) {
    this.colors = this.colorService.getColors();
  }

  ngOnInit(): void {
    // Subscribe to color changes
    this.colorService.colors$.subscribe(colors => {
      this.colors = colors;
      this.isCustomized = this.colorService.isCustomized();
    });
  }

  /**
   * Handle color change for a station
   */
  onColorChange(key: keyof StationColorConfig, color: string): void {
    const updates: Partial<StationColorConfig> = { [key]: color };
    this.colorService.setColors(updates);
  }

  /**
   * Reset all colors to defaults
   */
  resetToDefaults(): void {
    if (this.showResetConfirm) {
      this.colorService.resetToDefaults();
      this.showResetConfirm = false;
    } else {
      this.showResetConfirm = true;
      // Auto-hide confirmation after 3 seconds
      setTimeout(() => {
        this.showResetConfirm = false;
      }, 3000);
    }
  }

  /**
   * Cancel reset confirmation
   */
  cancelReset(): void {
    this.showResetConfirm = false;
  }

  /**
   * Get preview style for a color
   */
  getPreviewStyle(color: string): { [key: string]: string } {
    return {
      'background': color,
      'box-shadow': `0 0 16px ${color}66, inset 0 0 8px ${color}33`
    };
  }

  /**
   * Get text color based on background brightness
   */
  getTextColor(bgColor: string): string {
    // Convert hex to RGB
    const r = parseInt(bgColor.slice(1, 3), 16);
    const g = parseInt(bgColor.slice(3, 5), 16);
    const b = parseInt(bgColor.slice(5, 7), 16);

    // Calculate brightness (0-255)
    const brightness = (r * 299 + g * 587 + b * 114) / 1000;

    // Return white for dark colors, black for light colors
    return brightness > 128 ? '#000000' : '#ffffff';
  }
}
