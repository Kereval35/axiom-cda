interface BadgeProps {
  children: React.ReactNode;
  variant?: 'bbr' | 'ir' | 'fsh' | 'default';
}

export function Badge({ children, variant = 'default' }: BadgeProps) {
  const variants = {
    bbr: 'bg-gradient-to-r from-orange-200 to-orange-300 dark:from-orange-700 dark:to-orange-600 text-orange-900 dark:text-orange-100 border-orange-400 dark:border-orange-500 shadow-sm',
    ir: 'bg-gradient-to-r from-blue-200 to-blue-300 dark:from-blue-700 dark:to-blue-600 text-blue-900 dark:text-blue-100 border-blue-400 dark:border-blue-500 shadow-sm',
    fsh: 'bg-gradient-to-r from-green-200 to-green-300 dark:from-green-700 dark:to-green-600 text-green-900 dark:text-green-100 border-green-400 dark:border-green-500 shadow-sm',
    default: 'bg-gradient-to-r from-gray-200 to-gray-300 dark:from-gray-700 dark:to-gray-600 text-gray-900 dark:text-gray-100 border-gray-400 dark:border-gray-500 shadow-sm',
  };

  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border-2 ${variants[variant]}`}>
      {children}
    </span>
  );
}
