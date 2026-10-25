import {Cost} from './cost.model';
import {TravelEntry} from '../../travel-entry/travel-entry.model';

export interface Budget {
  id?: string;
  name: string;
  amount: number;
  cost: Cost[];
  travelentry: TravelEntry
}
