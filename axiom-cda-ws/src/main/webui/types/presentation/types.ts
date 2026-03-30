export type FragmentType = 'text' | 'code' | 'callout' | 'decision' | 'rule' | 'diagram' | 'list';

export interface Fragment {
  step: number;
  type: FragmentType;
  content: string | React.ReactNode;
  highlight?: boolean;
  variant?: 'info' | 'warning' | 'decision' | 'success';
}

export interface Slide {
  id: string;
  title: string;
  subtitle?: string;
  content: (string | React.ReactNode)[];
  fragments: Fragment[];
  stepsCount: number;
}

export interface DeckState {
  slideIndex: number;
  stepIndex: number;
}

export type NavigationAction = 'NEXT' | 'PREV' | 'GOTO_SLIDE';
