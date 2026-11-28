import { Backpack, Mountain, PhoneCall, RadioTower, WavesHorizontal, ZapOff, type LucideIcon } from 'lucide-react'

/**
 * Icons for the six flood-safety steps, from the Lucide icon set (open source,
 * consistent line style). Each has a tint: ink for most, teal where water is
 * the subject. Decorative; the step text carries the meaning.
 */
export const SAFETY_ICONS: { Icon: LucideIcon; tint: string }[] = [
  { Icon: RadioTower, tint: 'text-ink-950' },
  { Icon: Mountain, tint: 'text-ink-950' },
  { Icon: WavesHorizontal, tint: 'text-teal-600' },
  { Icon: ZapOff, tint: 'text-ink-950' },
  { Icon: Backpack, tint: 'text-ink-950' },
  { Icon: PhoneCall, tint: 'text-teal-600' },
]
