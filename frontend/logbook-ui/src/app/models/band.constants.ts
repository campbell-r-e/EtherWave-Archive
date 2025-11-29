/**
 * Amateur Radio Band Constants
 * Complete list of amateur radio bands from 160m to microwave
 */

export interface BandInfo {
  name: string;
  freqRangeKhz: { min: number; max: number };
  wavelength: string;
}

export const HAM_BANDS: BandInfo[] = [
  { name: '2200m', freqRangeKhz: { min: 135, max: 137 }, wavelength: '2200m' },
  { name: '630m', freqRangeKhz: { min: 472, max: 479 }, wavelength: '630m' },
  { name: '160m', freqRangeKhz: { min: 1800, max: 2000 }, wavelength: '160m' },
  { name: '80m', freqRangeKhz: { min: 3500, max: 4000 }, wavelength: '80m' },
  { name: '60m', freqRangeKhz: { min: 5330, max: 5405 }, wavelength: '60m' },
  { name: '40m', freqRangeKhz: { min: 7000, max: 7300 }, wavelength: '40m' },
  { name: '30m', freqRangeKhz: { min: 10100, max: 10150 }, wavelength: '30m' },
  { name: '20m', freqRangeKhz: { min: 14000, max: 14350 }, wavelength: '20m' },
  { name: '17m', freqRangeKhz: { min: 18068, max: 18168 }, wavelength: '17m' },
  { name: '15m', freqRangeKhz: { min: 21000, max: 21450 }, wavelength: '15m' },
  { name: '12m', freqRangeKhz: { min: 24890, max: 24990 }, wavelength: '12m' },
  { name: '10m', freqRangeKhz: { min: 28000, max: 29700 }, wavelength: '10m' },
  { name: '6m', freqRangeKhz: { min: 50000, max: 54000 }, wavelength: '6m' },
  { name: '2m', freqRangeKhz: { min: 144000, max: 148000 }, wavelength: '2m' },
  { name: '1.25m', freqRangeKhz: { min: 222000, max: 225000 }, wavelength: '1.25m' },
  { name: '70cm', freqRangeKhz: { min: 420000, max: 450000 }, wavelength: '70cm' },
  { name: '33cm', freqRangeKhz: { min: 902000, max: 928000 }, wavelength: '33cm' },
  { name: '23cm', freqRangeKhz: { min: 1240000, max: 1300000 }, wavelength: '23cm' },
  { name: '13cm', freqRangeKhz: { min: 2300000, max: 2450000 }, wavelength: '13cm' },
  { name: '9cm', freqRangeKhz: { min: 3300000, max: 3500000 }, wavelength: '9cm' },
  { name: '6cm', freqRangeKhz: { min: 5650000, max: 5925000 }, wavelength: '6cm' },
  { name: '3cm', freqRangeKhz: { min: 10000000, max: 10500000 }, wavelength: '3cm' },
];

/**
 * Get band name from frequency in kHz
 */
export function frequencyToBand(freqKhz: number): string | null {
  const band = HAM_BANDS.find(
    b => freqKhz >= b.freqRangeKhz.min && freqKhz <= b.freqRangeKhz.max
  );
  return band ? band.name : null;
}

/**
 * Get all band names as a simple array
 */
export function getAllBandNames(): string[] {
  return HAM_BANDS.map(b => b.name);
}
