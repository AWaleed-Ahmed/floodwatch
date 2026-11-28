/**
 * Ten of Pakistan's worst flood-hit cities, shown one at a time on the landing
 * page where the terrain ripples. This is fixed, historical context for the
 * showcase page only (no live data, no API call): the level is how severely the
 * place has flooded in recent major floods, and the year is its last major flood.
 * Live, per-day risk lives on the Dashboard and Risk pages.
 *
 * Based on widely reported impacts of the 2010 and 2022 floods (and Karachi's
 * 2020 urban flooding). Urdu names need a fluent reviewer like the rest of the
 * Urdu text.
 */
export interface FloodPlace {
  name: { en: string; ur: string }
  level: 'HIGH' | 'MEDIUM'
  year: number
}

export const FLOOD_HISTORY: FloodPlace[] = [
  { name: { en: 'Dadu', ur: 'دادو' }, level: 'HIGH', year: 2022 },
  { name: { en: 'Jacobabad', ur: 'جیکب آباد' }, level: 'HIGH', year: 2022 },
  { name: { en: 'Nowshera', ur: 'نوشہرہ' }, level: 'HIGH', year: 2022 },
  { name: { en: 'Charsadda', ur: 'چارسدہ' }, level: 'HIGH', year: 2022 },
  { name: { en: 'Swat', ur: 'سوات' }, level: 'HIGH', year: 2022 },
  { name: { en: 'Rajanpur', ur: 'راجن پور' }, level: 'HIGH', year: 2022 },
  { name: { en: 'Dera Ghazi Khan', ur: 'ڈیرہ غازی خان' }, level: 'MEDIUM', year: 2022 },
  { name: { en: 'Sukkur', ur: 'سکھر' }, level: 'MEDIUM', year: 2022 },
  { name: { en: 'Larkana', ur: 'لاڑکانہ' }, level: 'MEDIUM', year: 2022 },
  { name: { en: 'Karachi', ur: 'کراچی' }, level: 'MEDIUM', year: 2020 },
]
