import React from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { pageVariants, pageVariantsReduced } from '../../utils/motion';

export const PageTransition: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.div
      variants={shouldReduceMotion ? pageVariantsReduced : pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      className="flex-1 flex flex-col"
    >
      {children}
    </motion.div>
  );
};
