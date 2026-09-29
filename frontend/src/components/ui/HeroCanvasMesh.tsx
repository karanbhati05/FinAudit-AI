import React, { useEffect, useRef } from 'react';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  alpha: number;
}

export const HeroCanvasMesh: React.FC<{ className?: string }> = ({ className = '' }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { alpha: true });
    if (!ctx) return;

    let animationFrameId: number;
    let lastTime = 0;
    const targetFps = 30; // Capped frame rate to conserve CPU and frame budget
    const frameInterval = 1000 / targetFps;
    let isTabVisible = !document.hidden;

    // Detect prefers-reduced-motion
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Handle Resize
    const resize = () => {
      if (!canvas) return;
      const rect = canvas.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      ctx.scale(dpr, dpr);
    };

    resize();
    window.addEventListener('resize', resize);

    // Particle nodes for subtle compliance network grid
    const particles: Particle[] = [];
    const count = 32;

    const initParticles = () => {
      particles.length = 0;
      const rect = canvas.getBoundingClientRect();
      for (let i = 0; i < count; i++) {
        particles.push({
          x: Math.random() * rect.width,
          y: Math.random() * rect.height,
          vx: (Math.random() - 0.5) * 0.4,
          vy: (Math.random() - 0.5) * 0.4,
          radius: Math.random() * 1.5 + 1,
          alpha: Math.random() * 0.35 + 0.15,
        });
      }
    };

    initParticles();

    // Visibility change handler (pauses loop when tab is backgrounded)
    const handleVisibility = () => {
      isTabVisible = !document.hidden;
      if (isTabVisible && !prefersReducedMotion) {
        lastTime = performance.now();
        animationFrameId = requestAnimationFrame(render);
      }
    };

    document.addEventListener('visibilitychange', handleVisibility);

    // Render loop
    const render = (time: number) => {
      if (!isTabVisible || prefersReducedMotion) return;

      animationFrameId = requestAnimationFrame(render);

      const delta = time - lastTime;
      if (delta < frameInterval) return;
      lastTime = time - (delta % frameInterval);

      const rect = canvas.getBoundingClientRect();
      ctx.clearRect(0, 0, rect.width, rect.height);

      // Subtle gradient mesh ambient background
      const grad = ctx.createRadialGradient(
        rect.width * 0.75,
        rect.height * 0.3,
        10,
        rect.width * 0.75,
        rect.height * 0.3,
        rect.width * 0.6
      );
      grad.addColorStop(0, 'rgba(59, 130, 246, 0.08)');
      grad.addColorStop(1, 'rgba(59, 130, 246, 0.00)');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, rect.width, rect.height);

      // Update and draw particles
      ctx.fillStyle = 'rgba(59, 130, 246, 0.35)';
      ctx.strokeStyle = 'rgba(59, 130, 246, 0.06)';
      ctx.lineWidth = 1;

      for (let i = 0; i < particles.length; i++) {
        const p = particles[i];
        p.x += p.vx;
        p.y += p.vy;

        // Wrap boundaries
        if (p.x < 0) p.x = rect.width;
        if (p.x > rect.width) p.x = 0;
        if (p.y < 0) p.y = rect.height;
        if (p.y > rect.height) p.y = 0;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fill();

        // Connect nearby nodes with subtle lines
        for (let j = i + 1; j < particles.length; j++) {
          const p2 = particles[j];
          const dx = p.x - p2.x;
          const dy = p.y - p2.y;
          const distSq = dx * dx + dy * dy;
          if (distSq < 13000) { // ~114px
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
          }
        }
      }
    };

    if (prefersReducedMotion) {
      // Draw single static frame
      const rect = canvas.getBoundingClientRect();
      const grad = ctx.createRadialGradient(
        rect.width * 0.75,
        rect.height * 0.3,
        10,
        rect.width * 0.75,
        rect.height * 0.3,
        rect.width * 0.6
      );
      grad.addColorStop(0, 'rgba(59, 130, 246, 0.06)');
      grad.addColorStop(1, 'rgba(59, 130, 246, 0.00)');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, rect.width, rect.height);
    } else {
      animationFrameId = requestAnimationFrame(render);
    }

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', resize);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      className={`absolute inset-0 w-full h-full pointer-events-none ${className}`}
    />
  );
};
