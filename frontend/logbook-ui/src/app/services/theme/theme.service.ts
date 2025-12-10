import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type Theme = 'light' | 'dark' | 'high-contrast';

/**
 * GEKHoosier QSO Suite - Theme Management Service
 *
 * Manages dark/light theme switching with localStorage persistence
 */
@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'gekhoosier-theme';
  private themeSubject: BehaviorSubject<Theme>;
  public theme$: Observable<Theme>;

  constructor() {
    // Load saved theme or default to light
    const savedTheme = this.getSavedTheme();
    this.themeSubject = new BehaviorSubject<Theme>(savedTheme);
    this.theme$ = this.themeSubject.asObservable();

    // Apply initial theme
    this.applyTheme(savedTheme);
  }

  /**
   * Get current theme
   */
  getCurrentTheme(): Theme {
    return this.themeSubject.value;
  }

  /**
   * Toggle between light and dark theme
   */
  toggleTheme(): void {
    const newTheme: Theme = this.themeSubject.value === 'light' ? 'dark' : 'light';
    this.setTheme(newTheme);
  }

  /**
   * Set specific theme
   */
  setTheme(theme: Theme): void {
    this.themeSubject.next(theme);
    this.applyTheme(theme);
    this.saveTheme(theme);
  }

  /**
   * Check if current theme is dark
   */
  isDarkTheme(): boolean {
    return this.themeSubject.value === 'dark';
  }

  /**
   * Apply theme to document body
   */
  private applyTheme(theme: Theme): void {
    const body = document.body;

    // Remove existing theme classes
    body.classList.remove('light-theme', 'dark-theme', 'high-contrast-theme');

    // Add new theme class
    body.classList.add(`${theme}-theme`);

    // Set data attribute for CSS selectors
    body.setAttribute('data-theme', theme);

    // Update meta theme-color for mobile browsers
    this.updateMetaThemeColor(theme);
  }

  /**
   * Update meta theme-color tag for mobile browsers
   */
  private updateMetaThemeColor(theme: Theme): void {
    const metaThemeColor = document.querySelector('meta[name="theme-color"]');
    if (metaThemeColor) {
      let color: string;
      switch (theme) {
        case 'dark':
          color = '#1A1A1A';
          break;
        case 'high-contrast':
          color = '#000000';
          break;
        default:
          color = '#003F87';
      }
      metaThemeColor.setAttribute('content', color);
    }
  }

  /**
   * Save theme preference to localStorage
   */
  private saveTheme(theme: Theme): void {
    try {
      localStorage.setItem(this.THEME_KEY, theme);
    } catch (e) {
      console.warn('Failed to save theme preference:', e);
    }
  }

  /**
   * Load theme preference from localStorage
   */
  private getSavedTheme(): Theme {
    try {
      const saved = localStorage.getItem(this.THEME_KEY);
      if (saved === 'light' || saved === 'dark' || saved === 'high-contrast') {
        return saved as Theme;
      }
    } catch (e) {
      console.warn('Failed to load theme preference:', e);
    }

    // Default to system preference if available
    return this.getSystemPreference();
  }

  /**
   * Get system theme preference
   */
  private getSystemPreference(): Theme {
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  /**
   * Listen to system theme changes
   */
  watchSystemTheme(): void {
    if (window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

      mediaQuery.addEventListener('change', (e) => {
        // Only auto-switch if user hasn't explicitly set a preference
        if (!localStorage.getItem(this.THEME_KEY)) {
          const newTheme: Theme = e.matches ? 'dark' : 'light';
          this.setTheme(newTheme);
        }
      });
    }
  }
}
