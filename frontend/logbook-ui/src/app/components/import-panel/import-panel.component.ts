import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { LogService } from '../../services/log/log.service';
import { Log } from '../../models/log.model';
import { Station } from '../../models/station.model';

@Component({
  selector: 'app-import-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './import-panel.component.html',
  styleUrls: ['./import-panel.component.css']
})
export class ImportPanelComponent implements OnInit {
  currentLog: Log | null = null;
  stations: Station[] = [];
  selectedStation: number | null = null;
  selectedFile: File | null = null;
  importing = false;
  importResult: any = null;
  errorMessage: string | null = null;

  constructor(
    private apiService: ApiService,
    private logService: LogService
  ) {}

  ngOnInit(): void {
    this.loadCurrentLog();
    this.loadStations();
  }

  loadCurrentLog(): void {
    this.logService.currentLog$.subscribe({
      next: (log) => {
        this.currentLog = log;
      }
    });
  }

  loadStations(): void {
    this.apiService.getStations().subscribe({
      next: (stations) => {
        this.stations = stations;
        if (stations.length > 0 && !this.selectedStation && stations[0].id) {
          this.selectedStation = stations[0].id;
        }
      },
      error: (err) => console.error('Error loading stations:', err)
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file extension
      const filename = file.name.toLowerCase();
      if (filename.endsWith('.adi') || filename.endsWith('.adif')) {
        this.selectedFile = file;
        this.errorMessage = null;
        this.importResult = null;
      } else {
        this.errorMessage = 'Please select an ADIF file (.adi or .adif)';
        this.selectedFile = null;
        event.target.value = '';
      }
    }
  }

  importAdif(): void {
    if (!this.currentLog) {
      this.errorMessage = 'Please select a log first';
      return;
    }

    if (!this.selectedStation) {
      this.errorMessage = 'Please select a station';
      return;
    }

    if (!this.selectedFile) {
      this.errorMessage = 'Please select an ADIF file';
      return;
    }

    this.importing = true;
    this.errorMessage = null;
    this.importResult = null;

    this.apiService.importAdif(this.selectedFile, this.currentLog.id, this.selectedStation).subscribe({
      next: (result) => {
        this.importing = false;
        this.importResult = result;
        this.selectedFile = null;
        // Reset file input
        const fileInput = document.getElementById('adifFileInput') as HTMLInputElement;
        if (fileInput) {
          fileInput.value = '';
        }
      },
      error: (err) => {
        this.importing = false;
        this.errorMessage = err.error?.message || 'Failed to import ADIF file';
        console.error('Error importing ADIF:', err);
      }
    });
  }

  resetImport(): void {
    this.selectedFile = null;
    this.importResult = null;
    this.errorMessage = null;
    const fileInput = document.getElementById('adifFileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  getLogName(): string {
    if (!this.currentLog) return 'No log selected';
    return this.currentLog.contestName || 'Personal Log';
  }
}
