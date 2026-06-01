import type { SvgIconComponent } from '@mui/icons-material'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import RestaurantIcon from '@mui/icons-material/Restaurant'
import DirectionsCarIcon from '@mui/icons-material/DirectionsCar'
import LocalGasStationIcon from '@mui/icons-material/LocalGasStation'
import HomeIcon from '@mui/icons-material/Home'
import LocalHospitalIcon from '@mui/icons-material/LocalHospital'
import SchoolIcon from '@mui/icons-material/School'
import FlightIcon from '@mui/icons-material/Flight'
import FitnessCenterIcon from '@mui/icons-material/FitnessCenter'
import PetsIcon from '@mui/icons-material/Pets'
import PhoneAndroidIcon from '@mui/icons-material/PhoneAndroid'
import LocalGroceryStoreIcon from '@mui/icons-material/LocalGroceryStore'
import MovieIcon from '@mui/icons-material/Movie'
import LocalCafeIcon from '@mui/icons-material/LocalCafe'
import WorkIcon from '@mui/icons-material/Work'
import ChildCareIcon from '@mui/icons-material/ChildCare'
import SavingsIcon from '@mui/icons-material/Savings'
import ReceiptIcon from '@mui/icons-material/Receipt'
import SportsIcon from '@mui/icons-material/Sports'
import MusicNoteIcon from '@mui/icons-material/MusicNote'
import CardGiftcardIcon from '@mui/icons-material/CardGiftcard'
import BuildIcon from '@mui/icons-material/Build'
import BeachAccessIcon from '@mui/icons-material/BeachAccess'

export interface CategoryIcon {
  id: string
  Icon: SvgIconComponent
  label: string
}

export const CATEGORY_ICONS: CategoryIcon[] = [
  { id: 'shopping', Icon: ShoppingCartIcon, label: 'Shopping' },
  { id: 'food', Icon: RestaurantIcon, label: 'Food' },
  { id: 'car', Icon: DirectionsCarIcon, label: 'Car' },
  { id: 'gas', Icon: LocalGasStationIcon, label: 'Gas' },
  { id: 'home', Icon: HomeIcon, label: 'Home' },
  { id: 'health', Icon: LocalHospitalIcon, label: 'Health' },
  { id: 'education', Icon: SchoolIcon, label: 'Education' },
  { id: 'travel', Icon: FlightIcon, label: 'Travel' },
  { id: 'fitness', Icon: FitnessCenterIcon, label: 'Fitness' },
  { id: 'pets', Icon: PetsIcon, label: 'Pets' },
  { id: 'phone', Icon: PhoneAndroidIcon, label: 'Phone' },
  { id: 'groceries', Icon: LocalGroceryStoreIcon, label: 'Groceries' },
  { id: 'movies', Icon: MovieIcon, label: 'Movies' },
  { id: 'coffee', Icon: LocalCafeIcon, label: 'Coffee' },
  { id: 'work', Icon: WorkIcon, label: 'Work' },
  { id: 'kids', Icon: ChildCareIcon, label: 'Kids' },
  { id: 'savings', Icon: SavingsIcon, label: 'Savings' },
  { id: 'bills', Icon: ReceiptIcon, label: 'Bills' },
  { id: 'sports', Icon: SportsIcon, label: 'Sports' },
  { id: 'music', Icon: MusicNoteIcon, label: 'Music' },
  { id: 'gifts', Icon: CardGiftcardIcon, label: 'Gifts' },
  { id: 'repairs', Icon: BuildIcon, label: 'Repairs' },
  { id: 'vacation', Icon: BeachAccessIcon, label: 'Vacation' },
]

export function getCategoryIcon(id: string | null | undefined): SvgIconComponent | null {
  if (!id) return null
  return CATEGORY_ICONS.find(i => i.id === id)?.Icon ?? null
}
