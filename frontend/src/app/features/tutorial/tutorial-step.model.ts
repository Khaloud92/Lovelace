export interface TutorialStep {
  title: string;
  description: string;
  icon: string;
  targetSelector?: string;
  mobileTargetSelector?: string;
  mobileOnly?: boolean;
  desktopOnly?: boolean;
  centerOnly?: boolean;
}
