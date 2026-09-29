import type { Transition, Variants } from 'framer-motion';

/**
 * Standard enterprise audit easing curve (no bounce, no heavy overshoot)
 */
export const AUDIT_EASE = [0.16, 1, 0.3, 1] as const;

export const TRANSITION_FAST: Transition = {
  duration: 0.18,
  ease: AUDIT_EASE,
};

export const TRANSITION_NORMAL: Transition = {
  duration: 0.24,
  ease: AUDIT_EASE,
};

export const TRANSITION_PAGE: Transition = {
  duration: 0.22,
  ease: AUDIT_EASE,
};

export const pageVariants: Variants = {
  initial: {
    opacity: 0,
    y: 6,
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: TRANSITION_PAGE,
  },
  exit: {
    opacity: 0,
    y: -4,
    transition: { duration: 0.16, ease: AUDIT_EASE },
  },
};

export const pageVariantsReduced: Variants = {
  initial: { opacity: 1, y: 0 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 1, y: 0 },
};
