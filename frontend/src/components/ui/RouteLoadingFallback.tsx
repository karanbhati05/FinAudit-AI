import React from 'react';

export const RouteLoadingFallback: React.FC = () => {
  return (
    <div className="flex-1 min-h-[60vh] flex flex-col items-center justify-center p-8">
      <div className="relative flex items-center justify-center">
        <div className="w-10 h-10 rounded-full border-2 border-accent/20 border-t-accent animate-spin" />
        <div className="absolute w-5 h-5 rounded-full bg-accent/10" />
      </div>
      <p className="mt-4 text-caption text-secondary font-mono tracking-wider uppercase animate-pulse">
        Loading module...
      </p>
    </div>
  );
};
