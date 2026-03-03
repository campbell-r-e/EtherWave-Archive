// Minimal Leaflet stub for jsdom test environment.
// Leaflet uses browser APIs (canvas, SVG) that are not available in jsdom.

const noop = () => {};
const returnThis = function (this: any) { return this; };

const mapInstance = {
  setView: returnThis,
  addLayer: returnThis,
  removeLayer: returnThis,
  remove: noop,
  on: returnThis,
  off: returnThis,
  fitBounds: noop,
  getBounds: () => ({ toBBoxString: () => '0,0,0,0' }),
  getZoom: () => 10,
  invalidateSize: noop,
  panTo: noop,
};

export const map = jest.fn(() => mapInstance);

export const tileLayer = jest.fn(() => ({
  addTo: returnThis,
}));

export const marker = jest.fn(() => ({
  addTo: returnThis,
  setPopupContent: returnThis,
  bindPopup: returnThis,
  on: returnThis,
}));

export const circle = jest.fn(() => ({
  addTo: returnThis,
}));

export const polygon = jest.fn(() => ({
  addTo: returnThis,
}));

export const layerGroup = jest.fn(() => ({
  addTo: returnThis,
  addLayer: noop,
  removeLayer: noop,
  clearLayers: noop,
  getLayers: () => [],
}));

export const latLng = jest.fn((lat: number, lng: number) => ({ lat, lng }));

export const latLngBounds = jest.fn(() => ({
  isValid: () => true,
  toBBoxString: () => '0,0,0,0',
}));

export const icon = jest.fn(() => ({}));

export const divIcon = jest.fn(() => ({}));

export class DivIcon {}
export class Icon {
  static Default = { mergeOptions: noop };
}
export class LatLng {
  lat = 0;
  lng = 0;
}
export class LatLngBounds {}

const leaflet = {
  map,
  tileLayer,
  marker,
  circle,
  polygon,
  layerGroup,
  latLng,
  latLngBounds,
  icon,
  divIcon,
  DivIcon,
  Icon,
  LatLng,
  LatLngBounds,
};

export default leaflet;
