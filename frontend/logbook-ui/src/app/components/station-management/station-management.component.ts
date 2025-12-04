import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Station } from '../../models/station.model';

@Component({
  selector: 'app-station-management',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './station-management.component.html',
  styleUrls: ['./station-management.component.css']
})
export class StationManagementComponent implements OnInit {
  stations: Station[] = [];
  loading = true;
  showForm = false;
  editMode = false;
  currentStation: Station = this.getEmptyStation();

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadStations();
  }

  loadStations(): void {
    this.loading = true;
    this.apiService.getStations().subscribe({
      next: (stations) => {
        this.stations = stations;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading stations:', err);
        this.loading = false;
      }
    });
  }

  getEmptyStation(): Station {
    return {
      stationName: '',
      name: '',
      callsign: '',
      gridSquare: '',
      latitude: 0,
      longitude: 0,
      power: 100,
      antenna: '',
      comments: ''
    };
  }

  showAddForm(): void {
    this.currentStation = this.getEmptyStation();
    this.editMode = false;
    this.showForm = true;
  }

  editStation(station: Station): void {
    this.currentStation = { ...station };
    this.editMode = true;
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.currentStation = this.getEmptyStation();
  }

  saveStation(): void {
    if (!this.isFormValid()) {
      alert('Please fill in all required fields (Name and Callsign)');
      return;
    }

    if (this.editMode && this.currentStation.id) {
      this.apiService.updateStation(this.currentStation.id, this.currentStation).subscribe({
        next: (updated) => {
          const index = this.stations.findIndex(s => s.id === updated.id);
          if (index !== -1) {
            this.stations[index] = updated;
          }
          this.cancelForm();
        },
        error: (err) => {
          console.error('Error updating station:', err);
          alert('Error updating station');
        }
      });
    } else {
      this.apiService.createStation(this.currentStation).subscribe({
        next: (created) => {
          this.stations.unshift(created);
          this.cancelForm();
        },
        error: (err) => {
          console.error('Error creating station:', err);
          alert('Error creating station');
        }
      });
    }
  }

  deleteStation(station: Station): void {
    if (!station.id) return;

    if (!confirm(`Delete station "${station.name}" (${station.callsign})?`)) {
      return;
    }

    this.apiService.deleteStation(station.id).subscribe({
      next: () => {
        this.stations = this.stations.filter(s => s.id !== station.id);
      },
      error: (err) => {
        console.error('Error deleting station:', err);
        alert('Error deleting station');
      }
    });
  }

  isFormValid(): boolean {
    return !!(this.currentStation.name && this.currentStation.callsign);
  }

  /**
   * Get station color based on ARRL Field Day conventions
   * Station 1 = Blue, Station 2 = Red, GOTA = Green
   */
  getStationColor(station: Station): string {
    // GOTA stations are always green
    if (station.isGota) {
      return '#00AA00';
    }

    // Station number-based colors
    if (station.stationNumber) {
      const colors: { [key: number]: string } = {
        1: '#0066CC',  // Blue
        2: '#CC0000',  // Red
        3: '#FF6600',  // Orange
        4: '#9900CC',  // Purple
        5: '#00CCCC',  // Cyan
        6: '#CCCC00',  // Yellow
      };
      return colors[station.stationNumber] || '#666666'; // Gray for others
    }

    // Default gray for unassigned stations
    return '#666666';
  }

  /**
   * Get human-readable color name
   */
  getStationColorName(station: Station): string {
    if (station.isGota) {
      return 'Green (GOTA)';
    }

    if (station.stationNumber) {
      const names: { [key: number]: string } = {
        1: 'Blue',
        2: 'Red',
        3: 'Orange',
        4: 'Purple',
        5: 'Cyan',
        6: 'Yellow',
      };
      return names[station.stationNumber] || 'Gray';
    }

    return 'Unassigned';
  }
}
